package org.hpcclab.oaas.crm.observe;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.primitive.LongDoubleMaps;
import org.eclipse.collections.api.map.primitive.LongDoubleMap;
import org.hpcclab.oaas.crm.CrmConfig;
import org.hpcclab.oaas.crm.OprcComponent;
import org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.DataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hpcclab.oaas.crm.observe.CrPerformanceMetrics.addingMerge;

@ApplicationScoped
public class PrometheusCrMetricObserver implements CrMetricObserver {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusCrMetricObserver.class);
  WebClient webClient;
  String prometheusUrl;

  @Inject
  public PrometheusCrMetricObserver(WebClient webClient,
                                    CrmConfig config) {
    this.webClient = webClient;
    prometheusUrl = config.promUrl();
  }

  @Override
  public Map<String, CrPerformanceMetrics> observe() {
    var scope = createScope();
    var cpuJson = loadCPU(scope);
    var cpuMetricMap = parseResp(cpuJson, "pod");
    var memJson = loadMem(scope);
    var memMetricMap = parseResp(memJson, "pod");
    var rpsJson = loadRpsForRevision(scope);
    var rpsMetricMap = parseResp(rpsJson, "revision_name");
    var latencyJson = loadLatencyForRevision(scope);
    var latencyMetricMap = parseResp(latencyJson, "revision_name");
    Map<String, Map<OprcComponent, List<DataPoint>>> cpuCore = Maps.mutable.empty();
    Map<String, Map<String, List<DataPoint>>> cpuFn = Maps.mutable.empty();
    extract(cpuMetricMap, cpuCore, cpuFn);
    Map<String, Map<OprcComponent, List<DataPoint>>> memCore = Maps.mutable.empty();
    Map<String, Map<String, List<DataPoint>>> memFn = Maps.mutable.empty();
    extract(memMetricMap, memCore, memFn);
    Map<String, Map<OprcComponent, List<DataPoint>>> rpsCore = Maps.mutable.empty();
    Map<String, Map<String, List<DataPoint>>> rpsFn = Maps.mutable.empty();
    extract(rpsMetricMap, rpsCore, rpsFn);
    Map<String, Map<OprcComponent, List<DataPoint>>> latencyCore = Maps.mutable.empty();
    Map<String, Map<String, List<DataPoint>>> latencyFn = Maps.mutable.empty();
    extract(latencyMetricMap, latencyCore, latencyFn);

    Map<String, CrPerformanceMetrics> mergeMetrics = Maps.mutable.empty();
    for (String key : cpuCore.keySet()) {
      Map<OprcComponent, CrPerformanceMetrics.SvcPerformanceMetrics> coreMetrics = merge(
        cpuCore.getOrDefault(key, Map.of()),
        memCore.getOrDefault(key, Map.of()),
        rpsCore.getOrDefault(key, Map.of()),
        latencyCore.getOrDefault(key, Map.of())
      );

      Map<String, CrPerformanceMetrics.SvcPerformanceMetrics> fnMetrics = merge(
        cpuFn.getOrDefault(key, Map.of()),
        memFn.getOrDefault(key, Map.of()),
        rpsFn.getOrDefault(key, Map.of()),
        latencyFn.getOrDefault(key, Map.of())
      );

      mergeMetrics.put(key, new CrPerformanceMetrics(coreMetrics, fnMetrics));
    }
    return mergeMetrics;
  }

  private <K> Map<K, CrPerformanceMetrics.SvcPerformanceMetrics> merge(
    Map<K, List<DataPoint>> currentCpu,
    Map<K, List<DataPoint>> currentMem,
    Map<K, List<DataPoint>> currentRps,
    Map<K, List<DataPoint>> currentLatency
  ) {
    Map<K, CrPerformanceMetrics.SvcPerformanceMetrics> coreMetrics = Maps.mutable.empty();
    for (K oprcComponent : currentCpu.keySet()) {
      coreMetrics.put(oprcComponent, new CrPerformanceMetrics.SvcPerformanceMetrics(
        currentCpu.getOrDefault(oprcComponent, List.of()),
        currentMem.getOrDefault(oprcComponent, List.of()),
        currentRps.getOrDefault(oprcComponent, List.of()),
        currentLatency.getOrDefault(oprcComponent, List.of())
      ));
    }
    return coreMetrics;
  }

  private void extract(Map<String, List<DataPoint>> metricMap,
                       Map<String, Map<OprcComponent, List<DataPoint>>> groupByCrCoreMap,
                       Map<String, Map<String, List<DataPoint>>> groupByCrFnMap) {
    for (var entry : metricMap.entrySet()) {
      var splitKey = entry.getKey().split("-");
      var crid = splitKey[1];
      var currentCpuCore = groupByCrCoreMap.computeIfAbsent(crid, k -> Maps.mutable.empty());
      var currentCpuFn = groupByCrFnMap.computeIfAbsent(crid, k -> Maps.mutable.empty());
      if (splitKey.length==2) continue;
      switch (splitKey[2]) {
        case "fn" -> {
          var fnKey = extractFnName(splitKey);
          currentCpuFn.compute(fnKey, (k,v) -> {
            if (v == null) return entry.getValue();
            return addingMerge(v, entry.getValue());
          });
        }
        case "invoker" -> currentCpuCore.compute(OprcComponent.INVOKER, (k, v) -> {
          if (v == null) return entry.getValue();
          return addingMerge(v, entry.getValue());
        });
        case "storage" -> currentCpuCore.compute(OprcComponent.STORAGE_ADAPTER, (k, v) -> {
            if (v == null) return entry.getValue();
            return addingMerge(v, entry.getValue());
          });
      }
    }
  }


  private String extractFnName(String[] splitKey) {
    StringBuilder name = new StringBuilder(splitKey[3]);
    int i = 4;
    while (i < splitKey.length && !splitKey[i].matches("\\d+")) {
      name.append(".").append(splitKey[i++]);
    }
    return name.toString();
  }

  Scope createScope() {
    var now = System.currentTimeMillis() / 1000;
    var offset = 180;
    return new Scope(now - offset, now, 20);
  }

  public JsonObject loadCPU(Scope scope) {
    return query("""
      sum by (pod) (rate(container_cpu_usage_seconds_total{pod=~"cr-.*"}[1m]))\
      """, scope);
  }

  public JsonObject loadMem(Scope scope) {
    return query("""
      sum by (pod) (container_memory_usage_bytes{pod=~"cr-.*"})\
      """, scope);
  }

  public JsonObject loadRpsForRevision(Scope scope) {
    return query("""
      rate(activator_request_count{revision_name=~"cr-.*"}[1m])\
      """, scope);
  }
  public JsonObject loadRpsForInvoker(Scope scope) {
    return query("""
      sum by (crId) (rate(oprc_invocation_seconds_count[1m]))\
      """, scope);
  }

  public JsonObject loadLatencyForRevision(Scope scope) {
    return query("""
      histogram_quantile(0.99, sum by (revision_name, le) (rate(activator_request_latencies_bucket{revision_name=~"cr-.*"}[1m])))\
      """, scope);
  }

  public JsonObject loadLatencyForInvoker(Scope scope) {
    return query("""
      histogram_quantile(0.99, sum by (crId, le) (rate(oprc_invocation_seconds_bucket[1m])))\
      """, scope);
  }

  public JsonObject query(String query, Scope scope) {
    Map<String, String> queryParam = Map.of(
      "query", query,
      "start", String.valueOf(scope.start),
      "end", String.valueOf(scope.end),
      "step", String.valueOf(scope.step)
    );
    HttpRequest<Buffer> request = webClient.getAbs(prometheusUrl + "/api/v1/query_range")
      .putHeader(HttpHeaders.ACCEPT.toString(), MediaType.APPLICATION_JSON);
    request
      .queryParams().addAll(queryParam);
    var resp = request.send()
      .await().indefinitely();
    if (resp.statusCode() > 300) {
      throw new RuntimeException("Response is not OK : " + resp.statusCode() + " :: " + resp.bodyAsString());
    }

    return resp.bodyAsJsonObject();
  }

  public record Scope(
    long start,
    long end,
    int step) {
  }


  Map<String, List<DataPoint>> parseResp(JsonObject resp, String keyName) {
    var data = resp.getJsonObject("data");
    var results = data.getJsonArray("result");
    Map<String, List<DataPoint>> map = Maps.mutable.empty();
    for (int i = 0; i < results.size(); i++) {
      var result = results.getJsonObject(i);
      var metric = result.getJsonObject("metric");
      JsonArray values = result.getJsonArray("values");
      List<DataPoint> dataPoints = Lists.mutable.empty();
      for (int j = 0; j < values.size(); j++) {
        var pair = values.getJsonArray(j);
        dataPoints.add(
          new DataPoint(
            (long) (pair.getDouble(0) * 1000),
            Double.parseDouble(pair.getString(1))
          )
        );
      }
      var key = metric.getString(keyName);
      map.put(key, dataPoints);
    }
    return map;
  }

}
