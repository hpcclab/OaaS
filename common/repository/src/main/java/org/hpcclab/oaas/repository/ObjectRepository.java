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

  Uni<Pagination<OaasObject>> listByCls(List<String> clsKeys,
                                        long offset,
                                        int limit);

  Uni<Pagination<OaasObject>> sortedListByCls(List<String> clsKeys,
                                              String sortKey,
                                              boolean desc,
                                              long offset,
                                              int limit);
}
