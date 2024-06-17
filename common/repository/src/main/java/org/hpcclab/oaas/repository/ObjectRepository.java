package org.hpcclab.oaas.repository;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hpcclab.oaas.model.object.GOObject;

@RegisterForReflection(
  targets = GOObject.class,
  registerFullHierarchy = true
)
public interface ObjectRepository extends EntityRepository<String, GOObject> {

}
