package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Pawissanutt
 */
public class FileFnController
  extends AbstractFunctionController
  implements BuiltinFunctionController {

  final ContentUrlGenerator generator;
  final ObjectMapper objectMapper;

  public FileFnController(IdGenerator idGenerator, ObjectMapper mapper, ContentUrlGenerator urlGenerator, ObjectMapper objectMapper) {
    super(idGenerator, mapper);
    this.generator = urlGenerator;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void validate(InvocationCtx ctx) {

  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    var main = ctx.getMain();
    String keyString = ctx.getArgs().get("key");
    if (keyString == null) throw new InvocationException("key must be specified", 400);
    String[] keys = keyString.split(",");
    boolean pub = Boolean.parseBoolean(ctx.getArgs().getOrDefault("pub", "false"));
    boolean put = Boolean.parseBoolean(ctx.getArgs().getOrDefault("put", "false"));
    Map<String, String> keyToUrl;
    if (put) {
      keyToUrl = Stream.of(keys)
        .map(k -> Map.entry(k, generator.generatePutUrl(main, k, AccessLevel.UNIDENTIFIED, pub)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } else {
      keyToUrl = Stream.of(keys)
        .map(k -> Map.entry(k, generator.generateUrl(main, k, AccessLevel.UNIDENTIFIED, pub)))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    ObjectNode objectNode = objectMapper.valueToTree(keyToUrl);
    ctx.setRespBody(objectNode);
    return Uni.createFrom().item(ctx);
  }

  @Override
  public String getFnKey() {
    return "builtin.file";
  }
}
