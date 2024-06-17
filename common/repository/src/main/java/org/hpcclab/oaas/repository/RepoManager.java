package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.HasKey;

import java.util.HashMap;
import java.util.Map;

public abstract class RepoManager
  <K extends HasKey<String>, V, R extends EntityRepository<String, V>> {

  protected Map<String, R> repoMap = new HashMap<>();
  public abstract R createRepo(K cls);
  protected abstract K load(String clsKey);

  public R getOrCreate(String clsKey) {
    return repoMap.computeIfAbsent(clsKey, __ -> createRepo(load(clsKey)));
  }
  public R getOrCreate(K cls) {
    return repoMap.computeIfAbsent(cls.getKey(), k -> createRepo(cls));
  }
}
