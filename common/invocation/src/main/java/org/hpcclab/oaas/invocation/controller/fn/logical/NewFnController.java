package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import lombok.Builder;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class NewFnController extends AbstractFunctionController
  implements BuiltinFunctionController {
  final ContentUrlGenerator urlGenerator;

  public NewFnController(IdGenerator idGenerator,
                         ObjectMapper mapper,
                         ContentUrlGenerator urlGenerator) {
    super(idGenerator, mapper);
    this.urlGenerator = urlGenerator;
  }

  @Override
  protected void validate(InvocationCtx ctx) {
    // nothing
  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    var body = ctx.getRequest().body();
    ObjectConstructRequest req;
    if (body==null) {
      req = new ObjectConstructRequest(null, Set.of(), DSMap.of(), DSMap.of());
    } else {
      req = body.mapToObj(ObjectConstructRequest.class);
    }
    construct(ctx, req);
    return Uni.createFrom().item(ctx);
  }


  private void construct(InvocationCtx ctx,
                         ObjectConstructRequest construct) {
    OMeta meta = new OMeta();
    var obj = new GOObject(meta);
    var id = idGenerator.generate();
    meta.setId(id);
    meta.setCls(cls.getKey());
    obj.setData(new JsonBytes(construct.data));
    if (cls.getStateType()!=StateType.COLLECTION) {
      var verIds = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
        .toMap(KeySpecification::getName, __ -> id);
      meta.setVerIds(DSMap.wrap(verIds));
    }

    meta.setRevision(1);
    meta.setRefs(construct.refs());
    ctx.setOutput(obj);
    ctx.setStateOperations(List.of(
      SimpleStateOperation.createObjs(List.of(obj), cls)
    ));
    var fileKeys = construct.keys()==null ? Set.of():construct.keys();
    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> fileKeys.contains(k.getName()))
      .collect(KeySpecification::getName);
    if (ks.isEmpty()) {
      ctx.setRespBody(JsonBytes.EMPTY);
    } else {
      Map<String,String> keyToUrls = ks
        .toMap(
          k -> k,
          k ->urlGenerator.generatePutUrl(obj, k, AccessLevel.UNIDENTIFIED, true)
        );
      ObjectConstructResponse resp = new ObjectConstructResponse(keyToUrls);
      ctx.setRespBody(mapper.valueToTree(resp));
    }
    ctx.initLog().setStatus(InvocationStatus.SUCCEEDED);
  }

  @Override
  public String getFnKey() {
    return "builtin.new";
  }

  @Builder(toBuilder = true)
  public record ObjectConstructRequest(
    ObjectNode data,
    Set<String> keys,
    DSMap overrideUrls,
    DSMap refs
  ) {
    public static ObjectConstructRequest of(ObjectNode data) {
      return new ObjectConstructRequest(data, Set.of(), DSMap.of(), DSMap.of());
    }
  }

  @Builder(toBuilder = true)
  public record ObjectConstructResponse(
    Map<String, String> uploadUrls) {
  }


}
