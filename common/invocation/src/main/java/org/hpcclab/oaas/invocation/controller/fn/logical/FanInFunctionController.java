package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class FanInFunctionController extends AbstractFunctionController {

  public FanInFunctionController(IdGenerator idGenerator, ObjectMapper mapper) {
    super(idGenerator, mapper);
  }

  @Override
  protected void validate(InvocationCtx ctx) {
    if (ctx.getMain()==null) throw StdOaasException.format("main cannot be null");
    if (ctx.getRequest().body()==null) throw StdOaasException.format("body cannot be null");
  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    var req = ctx.getRequest();
    var body = req.body();
    var data = ctx.getMain().getData() == null?
      mapper.createObjectNode():
      ctx.getMain().getData();
    var stateNode = data.withObjectProperty("_fanIn");
    FanInStates fanInStates;
    FanInRequest fanInReq;
    try {
      fanInReq = mapper.treeToValue(body, FanInRequest.class);
      fanInStates = mapper.treeToValue(stateNode, FanInStates.class);
    } catch (JsonProcessingException e) {
      throw new InvocationException("Json parsing error", e);
    }
    FanInState state = fanInStates.states().get(fanInReq.id());
    FanInState newState;
    if (state==null) newState = new FanInState(
      fanInReq.request, fanInReq.totalRequire, Set.of(fanInReq.step()));
    else newState = new FanInState(
      state.request == null? fanInReq.request: state.request,
      state.totalRequire,
      Sets.mutable.of(fanInReq.step).withAll(state.done)
    );
    if (newState.totalRequire == newState.done().size()){
      ctx.getReqToProduce()
        .add(newState.request());
      fanInStates.states().remove(fanInReq.id);
    } else {
      fanInStates.states().put(fanInReq.id(), newState);
    }
    data.replace("_fanIn", mapper.valueToTree(fanInStates));
    ctx.getMain().setData(data);
    ctx.getStateOperations()
      .add(SimpleStateOperation.updateObjs(ctx.getMain(), cls));
    return Uni.createFrom().item(ctx);
  }


  public record FanInRequest(
    String id,
    InvocationRequest request,
    int totalRequire,
    int step) {
  }

  public record FanInStates(
    @JsonValue
    Map<String, FanInState> states) {
  }

  public record FanInState(
    InvocationRequest request,
    int totalRequire,
    Set<Integer> done) {
  }
}
