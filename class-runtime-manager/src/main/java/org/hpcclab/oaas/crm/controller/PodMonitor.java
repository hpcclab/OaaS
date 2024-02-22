package org.hpcclab.oaas.crm.controller;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
@Group("monitoring.coreos.com")
@Version("v1")
public class PodMonitor extends CustomResource<PodMonitor.PodMonitorSpec, PodMonitor.PodMonitorStatus> {

  @Override
  protected PodMonitorSpec initSpec() {
    return new PodMonitorSpec();
  }

  @Override
  protected PodMonitorStatus initStatus() {
    return new PodMonitorStatus();
  }

  @Override
  public String getKind() {
    return "PodMonitor";
  }

  @Override
  public String getApiVersion() {
    return "monitoring.coreos.com/v1";
  }

  public static class PodMonitorSpec{
    Selector selector;
    List<Endpoint> podMetricsEndpoints;



    public Selector getSelector() {
      return selector;
    }

    public void setSelector(Selector selector) {
      this.selector = selector;
    }

    public List<Endpoint> getPodMetricsEndpoints() {
      return podMetricsEndpoints;
    }

    public void setPodMetricsEndpoints(List<Endpoint> podMetricsEndpoints) {
      this.podMetricsEndpoints = podMetricsEndpoints;
    }
  }

  public record Selector(Map<String, String> matchLabels) {}

  public record Endpoint(String port, String path) {

  }

  public static class PodMonitorStatus{

  }
}
