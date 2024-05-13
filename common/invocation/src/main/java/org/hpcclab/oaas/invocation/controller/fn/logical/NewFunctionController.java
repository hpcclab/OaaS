package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.InvocationCtx;
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
import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
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
      req = new ObjectConstructRequest(null, Set.of(), DSMap.of(),DSMap.of());
    } else {
      try {
        req = mapper.treeToValue(body, ObjectConstructRequest.class);
      } catch (JsonProcessingException e) {
        throw new FunctionValidationException("Cannot decode body to 'ObjectConstructRequest'", e);
      }
    }
    return construct(ctx, req);
  }


  private Uni<InvocationCtx> construct(InvocationCtx ctx,
                                       ObjectConstructRequest construct) {
    var obj = new OObject();
    var id = idGenerator.generate();
    obj.setId(id);
    obj.setCls(cls.getKey());
    obj.setData(construct.data());
    var state = new OaasObjectState();
    if (cls.getStateType()!=StateType.COLLECTION) {
      var verIds = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
        .toMap(KeySpecification::getName, __ -> id);
      state.setVerIds(DSMap.wrap(verIds));
    }
    state.setOverrideUrls(construct.overrideUrls());

    obj.setState(state);
    obj.setRevision(1);
    obj.setRefs(construct.refs());
    ctx.setOutput(obj);
    ctx.setStateOperations(List.of(
      SimpleStateOperation.createObjs(List.of(obj), cls)
    ));
    var fileKeys = construct.keys() == null? Set.of() : construct.keys();
    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> fileKeys.contains(k.getName()))
      .collect(KeySpecification::getName);
    if (ks.isEmpty()) {
      ctx.setRespBody(null);
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

  public record ObjectConstructResponse(
    OObject object,
    Map<String, String> uploadUrls) {
  }


}
