package org.hpcclab.oaas.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PresignGeneratorPool {
  Map<String, PresignGenerator> generatorMap = new ConcurrentHashMap<>();
  Map<String, PresignGenerator> pubGeneratorMap = new ConcurrentHashMap<>();

  public static final String DEFAULT = "default";

  public PresignGenerator getGenerator(){
    return generatorMap.get(DEFAULT);
  }

  public PresignGenerator getGenerator(String name){
    return generatorMap.get(name);
  }
  public PresignGenerator getPublicGenerator(){
    return pubGeneratorMap.get(DEFAULT);
  }
  public PresignGenerator getPubGenerator(String name){
    return pubGeneratorMap.get(name);
  }

  public void put(String name, PresignGenerator generator) {
    generatorMap.put(name, generator);
  }

  public void putPub(String name, PresignGenerator generator) {
    pubGeneratorMap.put(name, generator);
  }
}
