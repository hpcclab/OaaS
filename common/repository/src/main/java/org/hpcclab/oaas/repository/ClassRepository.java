package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.OaasFunction;

import java.util.Map;

public interface ClassRepository extends EntityRepository<String, OaasClass> {
  Map<String,OaasClass> resolveInheritance (Map<String,OaasClass> clsMap);
}
