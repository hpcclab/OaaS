package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.applier.logical.ObjectConstructRequest;
import org.hpcclab.oaas.invocation.applier.logical.ObjectConstructResponse;
import org.hpcclab.oaas.invocation.controller.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.LogicalFunctionController;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.List;

/**
 * @author Pawissanutt
 */
@Dependent
public class NewFunctionController extends AbstractFunctionController
  implements LogicalFunctionController {
  DataUrlAllocator allocator;

  public NewFunctionController(IdGenerator idGenerator,
                               ObjectMapper mapper,
                               DataUrlAllocator allocator) {
    super(idGenerator, mapper);
    this.allocator = allocator;
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
      req = new ObjectConstructRequest();
    } else {
      try {
        req = mapper.treeToValue(body, ObjectConstructRequest.class);
      } catch (JsonProcessingException e) {
        throw new FunctionValidationException("Cannot decode body to 'ObjectConstructRequest'", e);
      }
    }
    if (ctx.getRequest().cls()!=null)
      req.setCls(ctx.getRequest().cls());
    return construct(ctx, req);
  }


  private Uni<InvocationCtx> construct(InvocationCtx ctx,
                                       ObjectConstructRequest construct) {
    var obj = new OObject();
    var id = idGenerator.generate();
    obj.setId(idGenerator.generate());
    obj.setData(construct.getData());
    var state = new OaasObjectState();
    if (cls.getStateType()!=StateType.COLLECTION) {
      var verIds = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
        .toMap(KeySpecification::getName, __ -> id);
      state.setVerIds(DSMap.wrap(verIds));
    }
    state.setOverrideUrls(construct.getOverrideUrls());

    obj.setState(state);
    obj.setRevision(1);
    obj.setRefs(construct.getRefs());
    ctx.setOutput(obj);
    ctx.setStateOperations(List.of(
      SimpleStateOperation.createObjs(List.of(obj), cls)
    ));

    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> construct.getKeys().contains(k.getName()));
    if (ks.isEmpty()) {
      ctx.setRespBody(mapper.createObjectNode());
      return Uni.createFrom()
        .item(ctx);
    } else {
      DataAllocateRequest request = new DataAllocateRequest(
        obj.getId(),
        ks,
        cls.getStateSpec().getDefaultProvider(), true);
      return allocator.allocate(List.of(request))
        .map(list -> new ObjectConstructResponse(null, list.getFirst().getUrlKeys()))
        .map(resp -> ctx.setRespBody(mapper.valueToTree(resp)));
    }
  }

  @Override
  public String getFnKey() {
    return "builtin.logical.new";
  }
}
