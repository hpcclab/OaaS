package org.hpcclab.oaas.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.task.InvokingDetail;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.object.OOUpdate;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class MockOffLoader implements OffLoader {
  private static final Logger logger = LoggerFactory.getLogger(MockOffLoader.class);
  Mapper mapper = new DefaultMapper();

  public MockOffLoader() {
  }
  public MockOffLoader(Mapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Uni<OTaskCompletion> offload(InvokingDetail<?> invokingDetail) {
    if (mapper!=null) {
      var tc = mapper.apply(invokingDetail);
      logger.debug("invoke mapping {}\nto {}", invokingDetail, tc);
      return Uni.createFrom().item(tc);
    }
    return Uni.createFrom().nullItem();
  }

  public interface Mapper extends Function<InvokingDetail<?>, OTaskCompletion>{}

  public static class DefaultMapper implements Mapper {
    ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public OTaskCompletion apply(InvokingDetail<?> detail) {
      OTask task = (OTask) detail.getContent();
      OOUpdate mainUpdate = null;
      OOUpdate outUpdate = null;
      var bodySum = Optional.ofNullable(task.getReqBody())
        .map(JsonBytes::getNode)
        .stream()
        .flatMap(on -> StreamSupport
          .stream(Spliterators.spliteratorUnknownSize(on.elements(), Spliterator.ORDERED), false))
        .filter(JsonNode::isInt)
        .mapToInt(JsonNode::intValue)
        .sum();
      Optional<ObjectNode> mainDataOptional = Optional.ofNullable(task.getMain())
        .map(GOObject::getData)
        .map(JsonBytes::getNode);
      int n = mainDataOptional
        .map(on -> on.get("n").asInt())
        .orElse(0);
      var add = Integer.parseInt(task.getArgs().getOrDefault("ADD", "1"));

      var data = objectMapper.createObjectNode()
        .put("n", n + add + bodySum);
      if (!task.isImmutable()) {
        mainUpdate = new OOUpdate(data);
      }
      if (task.getOutput()!=null) {
        outUpdate = new OOUpdate(data);
      }

      return new OTaskCompletion()
        .setId(detail.getId())
        .setMain(mainUpdate)
        .setOutput(outUpdate)
        .setBody(new JsonBytes(data))
        .setSuccess(true);
    }
  }

  public static class Factory implements OffLoaderFactory {
    @Override
    public OffLoader create(OFunction function) {
      return new MockOffLoader();
    }
  }
}
