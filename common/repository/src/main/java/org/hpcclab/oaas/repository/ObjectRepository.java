package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.OaasObject;

public interface ObjectRepository extends EntityRepository<String, OaasObject> {
  Uni<FunctionExecContext> persistFromCtx(FunctionExecContext context);

  Pagination<OaasObject> listByCls(String clsName,
                                   long offset,
                                   int limit);
  Uni<OaasObject> createRootAndPersist(OaasObject object);
}
