package org.hpcclab.oaas.repository;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hpcclab.oaas.model.object.OObject;

@RegisterForReflection(
  targets = OObject.class,
  registerFullHierarchy = true
)
public interface ObjectRepository extends EntityRepository<String, OObject> {

}
