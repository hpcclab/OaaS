package org.hpcclab.oaas.repository;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.object.OaasObject;

import java.util.List;

@RegisterForReflection(
  targets = OaasObject.class,
  registerFullHierarchy = true
)
public interface ObjectRepository extends EntityRepository<String, OaasObject> {

}
