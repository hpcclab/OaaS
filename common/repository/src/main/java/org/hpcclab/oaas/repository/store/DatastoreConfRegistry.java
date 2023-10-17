package org.hpcclab.oaas.repository.store;

import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DatastoreConfRegistry {

  Map<String, DatastoreConf> confMap = new HashMap<>();

  public static DatastoreConfRegistry createDefault() {
    return new DatastoreConfRegistry("OPRC_DB_", "oprc.env");
  }

  public DatastoreConfRegistry(String prefix, String keyToLoad) {
    var rawConf = ConfigProvider.getConfig().getValue(keyToLoad, String.class);
    var map = Arrays.stream(rawConf.split("\\\n"))
      .map(line -> {
        var kv = line.split("=");
        return Map.entry(kv[0], kv[1]);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    load(prefix, map);
  }

  void load(String prefix, Map<String, String> mapToMerge) {
    mapToMerge.putAll(System.getenv());
    var storeSets = mapToMerge.keySet()
      .stream()
      .filter(k -> k.startsWith(prefix))
      .map(k -> k.substring(prefix.length()).split("_"))
      .filter(ks -> ks.length==2)
      .map(ks -> ks[0])
      .collect(Collectors.toSet());
    for (String storeSet : storeSets) {
      var name = storeSet;
      var storePrefix = (prefix + storeSet);
      var options = mapToMerge.entrySet()
        .stream()
        .filter(entry -> entry.getKey().startsWith(storePrefix) &&
          entry.getKey().substring(prefix.length()).split("_").length==2)
        .map(entry -> Map.entry(entry.getKey().substring(storePrefix.length() + 1), entry.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      var type = options.get("TYPE");
      options.remove("TYPE");
      var host = options.get("HOST");
      options.remove("HOST");
      var port = Integer.parseInt(options.get("PORT"));
      options.remove("PORT");
      var user = options.get("USER");
      options.remove("USER");
      var pass = options.getOrDefault("PASS", "");
      options.remove("PASS");
      confMap.put(name, new DatastoreConf(name, type, host, port, user, pass, options));
    }
  }

  public Map<String, DatastoreConf> getConfMap() {
    return confMap;
  }

  DatastoreConf get(String name) {
    return confMap.get(name);
  }
}
