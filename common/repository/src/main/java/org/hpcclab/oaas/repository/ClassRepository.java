package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.cls.OClass;

import java.util.List;

public interface ClassRepository extends CachedEntityRepository<String, OClass> {
  default List<OClass> listSubCls(String clsKey){
    throw new UnsupportedOperationException();
  }
}
