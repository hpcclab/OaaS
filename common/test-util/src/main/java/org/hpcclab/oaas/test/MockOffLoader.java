package org.hpcclab.oaas.test;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvokingDetail;
import org.hpcclab.oaas.invocation.OffLoader;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class MockOffLoader implements OffLoader {
private static final Logger logger = LoggerFactory.getLogger( MockOffLoader.class );
  Function<InvokingDetail<?>, TaskCompletion> mapper = new DefaultMapper();

  public void setMapper(Function<InvokingDetail<?>, TaskCompletion> mapper) {
    this.mapper = mapper;
  }

  @Override
  public Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail) {
    if (mapper!=null) {
      var tc = mapper.apply(invokingDetail);
      logger.debug("invoke mapping {} to {}", invokingDetail, tc);
      return Uni.createFrom().item(tc);
    }
    return Uni.createFrom().nullItem();
  }

  static class DefaultMapper implements Function<InvokingDetail<?>, TaskCompletion> {

    @Override
    public TaskCompletion apply(InvokingDetail<?> detail) {
      return new TaskCompletion()
        .setId(TaskIdentity.decode(detail.getId()))
        .setSuccess(true);
    }
  }
}
