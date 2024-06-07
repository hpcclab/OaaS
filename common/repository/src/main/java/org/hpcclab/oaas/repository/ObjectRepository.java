package org.hpcclab.oaas.repository;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;

@RegisterForReflection(
  targets = OObject.class,
  registerFullHierarchy = true
)
public interface ObjectRepository extends EntityRepository<String, GOObject> {

}
