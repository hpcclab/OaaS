package org.hpcclab.oaas.repository.store;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DatastoreConfRegistry {

  public static final String DEFAULT = "DEFAULT";
  public static final String NONE = "NONE";
  public static final String DEFAULT_ENV_PREFIX = "OPRC_DB_";

  Map<String, DatastoreConf> confMap = new HashMap<>();

  private static DatastoreConfRegistry instance;
  public static DatastoreConfRegistry getDefault() {
    if (instance== null)
      instance = new DatastoreConfRegistry(DEFAULT_ENV_PREFIX, "oprc.env");
    return instance;
  }

  public DatastoreConfRegistry(String prefix, String keyToLoad) {
    var rawConf = ConfigProvider.getConfig().getOptionalValue(keyToLoad, String.class)
      .orElse("");
    Map<String,String> map = Arrays.stream(rawConf.split("\\\n"))
      .map(line -> {
        var kv = line.split("=");
        if (kv.length == 2)
          return Map.entry(kv[0], kv[1]);
        return null;
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    load(prefix, map);
  }

  void load(String prefix, Map<String, String> mapToMerge) {
    mapToMerge.putAll(System.getenv());
    var storeSets = mapToMerge.keySet()
      .stream()
      .filter(k -> k.startsWith(prefix))
      .map(k -> k.substring(prefix.length()).split("_"))
      .filter(ks -> ks.length>=2)
      .map(ks -> ks[0])
      .collect(Collectors.toSet());
    for (String name : storeSets) {
      var storePrefix = prefix + name + "_";
      var options = mapToMerge.entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(storePrefix))
        .map(entry -> Map.entry(entry.getKey().substring(storePrefix.length()), entry.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      var type = options.get("TYPE");
      options.remove("TYPE");
      var host = options.get("HOST");
      options.remove("HOST");
      var port = Integer.parseInt(options.getOrDefault("PORT", "80"));
      options.remove("PORT");
      var user = options.get("USER");
      options.remove("USER");
      var pass = options.getOrDefault("PASS", "");
      options.remove("PASS");
      confMap.put(name, new DatastoreConf(name, type, host, port, user, pass, options));
    }
  }

  public Map<String, String> dump() {
    var map = new HashMap<String, String>();
    for (Map.Entry<String, DatastoreConf> entry : confMap.entrySet()) {
      var conf = entry.getValue();
      var key = entry.getKey();
      putIfNotNull(map, key + "_TYPE", conf.type());
      putIfNotNull(map, key + "_HOST", conf.host());
      putIfNotNull(map, key + "_PORT", conf.port());
      putIfNotNull(map, key + "_USER", conf.user());
      putIfNotNull(map, key + "_PASS", conf.pass());
      for (var optionEntry : conf.options().entrySet()) {
        putIfNotNull(map, key + "_" + optionEntry.getKey(), optionEntry.getValue());
      }
    }
    return map;
  }

  void putIfNotNull(Map<String,String> m, String k, Object v) {
    if (k == null) return;
    if (v == null) return;
    m.put(DatastoreConfRegistry.DEFAULT_ENV_PREFIX + k, String.valueOf(v));
  }

  public Map<String, DatastoreConf> getConfMap() {
    return confMap;
  }

  public DatastoreConf getOrDefault(String name) {
    if (name == null)
      return confMap.get(DEFAULT);
    if (name.equalsIgnoreCase(NONE))
      return null;
    if (confMap.containsKey(name))
      return confMap.get(name);
    return confMap.get(DEFAULT);
  }

  DatastoreConf get(String name) {
    return confMap.get(name);
  }
}
