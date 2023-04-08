package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.cls.OaasClass;

import java.util.List;

public interface ClassRepository extends EntityRepository<String, OaasClass> {

  default Uni<List<String>> listSubClsKeys(String clsKey){
    throw new UnsupportedOperationException();
  }

  default List<OaasClass> listSubCls(String clsKey){
    throw new UnsupportedOperationException();
  }
}
