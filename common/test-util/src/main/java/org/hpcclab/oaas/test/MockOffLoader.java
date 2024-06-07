package org.hpcclab.oaas.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.task.InvokingDetail;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.object.*;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;

public class MockOffLoader implements OffLoader {
  private static final Logger logger = LoggerFactory.getLogger(MockOffLoader.class);
  Function<InvokingDetail<?>, TaskCompletion> mapper = new DefaultMapper();

  public void setMapper(Function<InvokingDetail<?>, TaskCompletion> mapper) {
    this.mapper = mapper;
  }

  @Override
  public Uni<TaskCompletion> offload(InvokingDetail<?> invokingDetail) {
    if (mapper!=null) {
      var tc = mapper.apply(invokingDetail);
      logger.debug("invoke mapping {}\nto {}", invokingDetail, tc);
      return Uni.createFrom().item(tc);
    }
    return Uni.createFrom().nullItem();
  }

  static class DefaultMapper implements Function<InvokingDetail<?>, TaskCompletion> {

    @Override
    public TaskCompletion apply(InvokingDetail<?> detail) {
      return new TaskCompletion().setId(detail.getId()).setSuccess(true);
    }
  }

  public static class Factory implements OffLoaderFactory {

    public Factory() {
    }

    @Override
    public OffLoader create(OFunction function) {
      MockOffLoader mockOffLoader = new MockOffLoader();
      ObjectMapper objectMapper = new ObjectMapper();
      mockOffLoader.setMapper(detail -> {
        OTask task = (OTask) detail.getContent();
        OOUpdate mainUpdate = null;
        OOUpdate outUpdate = null;
        Optional<ObjectNode> jsonNodes = Optional.ofNullable(task.getMain())
          .map(JOObject::getData);
        int n = jsonNodes
          .map(on -> on.get("n").asInt())
          .orElse(0);
        var add = Integer.parseInt(task.getArgs().getOrDefault("ADD", "1"));

        var data = objectMapper.createObjectNode()
          .put("n", n + add);
        if (!task.isImmutable()) {
          mainUpdate = new OOUpdate(data);
        }
        if (task.getOutput()!=null) {
          outUpdate = new OOUpdate(data);
        }

        return new TaskCompletion()
          .setId(detail.getId())
          .setMain(mainUpdate)
          .setOutput(outUpdate)
          .setSuccess(true);
      });
      return mockOffLoader;
    }
  }
}
