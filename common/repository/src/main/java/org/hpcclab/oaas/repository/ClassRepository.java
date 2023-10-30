package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;

import java.util.List;

public interface ClassRepository extends CachedEntityRepository<String, OaasClass> {
  default List<OaasClass> listSubCls(String clsKey){
    throw new UnsupportedOperationException();
  }
}
