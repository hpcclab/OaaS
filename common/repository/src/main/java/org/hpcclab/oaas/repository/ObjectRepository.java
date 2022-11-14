package org.hpcclab.oaas.repository;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.object.OaasObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

public interface ObjectRepository extends EntityRepository<String, OaasObject> {
  default Uni<FunctionExecContext> persistFromCtx(FunctionExecContext context) {
    if (context.getFunction().getType()==FunctionType.MACRO) {
      var list = new ArrayList<>(context.getSubOutputs());
      list.add(context.getOutput());
      return persistAsync(list)
        .replaceWith(context);
    } else {
      return persistAsync(context.getOutput())
        .replaceWith(context);
    }
  }

  Uni<Pagination<OaasObject>> listByCls(List<String> clsKeys,
                                        long offset,
                                        int limit);

  Uni<Pagination<OaasObject>> sortedListByCls(List<String> clsKeys,
                                              String sortKey,
                                              boolean desc,
                                              long offset,
                                              int limit);
}
