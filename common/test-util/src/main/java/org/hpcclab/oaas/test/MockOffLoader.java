package org.hpcclab.oaas.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.task.InvokingDetail;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.object.OObject;
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
        int n = Optional.ofNullable(task.getMain())
          .map(OObject::getData)
          .map(on -> on.get("n").asInt())
          .orElse(0);
        if (task.getInputs()!=null && !task.getInputs().isEmpty()) {
          for (OObject input : task.getInputs()) {
            var ni = Optional.ofNullable(input)
              .map(OObject::getData)
              .map(on -> on.get("n").asInt())
              .orElse(0);
            n += ni;
          }
        }
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
