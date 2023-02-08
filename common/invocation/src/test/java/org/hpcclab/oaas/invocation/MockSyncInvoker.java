package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class MockSyncInvoker implements SyncInvoker {
private static final Logger logger = LoggerFactory.getLogger( MockSyncInvoker.class );
  Function<InvokingDetail<?>, TaskCompletion> mapper;

  public void setMapper(Function<InvokingDetail<?>, TaskCompletion> mapper) {
    this.mapper = mapper;
  }

  @Override
  public Uni<TaskCompletion> invoke(InvokingDetail<?> invokingDetail) {
    if (mapper!=null) {
      var tc = mapper.apply(invokingDetail);
      logger.debug("invoke mapping {} to {}", invokingDetail, tc);
      return Uni.createFrom().item(tc);
    }
    return Uni.createFrom().nullItem();
  }
}
