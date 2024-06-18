package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.Builder;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic.DataflowNode;
import org.hpcclab.oaas.invocation.transform.ODataTransformer;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public class DataflowOrchestrator {
  private static final Logger logger = LoggerFactory.getLogger(DataflowOrchestrator.class);
  final LocationAwareInvocationForwarder invocationForwarder;
  final IdGenerator idGenerator;
  final ObjectMapper mapper = new ObjectMapper();

  public DataflowOrchestrator(LocationAwareInvocationForwarder invocationForwarder,
                              IdGenerator idGenerator) {
    this.invocationForwarder = invocationForwarder;
    this.idGenerator = idGenerator;
  }

  public Uni<InvocationCtx> execute(final InvocationCtx ctx,
                                    final DataflowSemantic dataflowSemantic) {
    var stepState = new StepState[dataflowSemantic.getAllNode().size() + 1];
    stepState[0] = new StepState(true, true,
      ctx.getMain(), ctx.getRequest().body(), null);
    for (int i = 1; i < stepState.length; i++) {
      stepState[i] = StepState.NULL;
    }
    var state = new DataflowState(stepState);
    return branch(ctx, dataflowSemantic.getRootNode(), state)
      .map(__ -> createResp(ctx, dataflowSemantic, state));
  }

  Uni<DataflowState> branch(InvocationCtx ctx,
                            DataflowNode node,
                            DataflowState state) {
    Set<DataflowNode> next = node.next();
    return Multi.createFrom().iterable(next)
      .filter(n -> ready(state, n))
      .onItem()
      .transformToUniAndMerge(nextNode -> {
        InvocationRequest req = createReq(nextNode, ctx, state);
        return invocationForwarder.invoke(req)
          .flatMap(resp -> updateState(ctx, nextNode, state, resp));
      })
      .collect()
      .last()
      .replaceWith(state);
  }

  Uni<DataflowState> updateState(InvocationCtx ctx,
                                 DataflowNode node,
                                 DataflowState state,
                                 InvocationResponse resp) {
    StepState stepState = state.stepStates[node.stepIndex() + 1];
    state.stepStates[node.stepIndex() + 1] = stepState
      .toBuilder()
      .completed(true)
      .sent(true)
      .obj(resp.output())
      .body(resp.body())
      .resp(resp)
      .build();
    return branch(ctx, node, state);
  }

  InvocationCtx createResp(InvocationCtx ctx,
                           DataflowSemantic semantic,
                           DataflowState state) {
    DataflowNode endNode = semantic.getEndNode();
    int mainRefStepIndex = endNode.mainRefStepIndex();
    if (mainRefStepIndex >= -1) {
      var obj = state.stepStates[mainRefStepIndex + 1].obj;
      ctx.setOutput(obj);
    }
    var body = createReqBody(endNode, state);
    ctx.setRespBody(body.getNode());
    return ctx;
  }

  InvocationRequest createReq(DataflowSemantic.DataflowNode node,
                              InvocationCtx ctx,
                              DataflowState state) {
    Dataflows.Step step = node.step();
    String mainId = null;
    String mainCls = null;
    if (node.mainRefStepIndex() >= -1) {
      StepState stepState = state.stepStates()[node.mainRefStepIndex() + 1];
      if (stepState.obj()!=null) {
        mainId = stepState.obj().getKey();
        mainCls = stepState.obj().getMeta().getCls();
      }
    }
    var invId = idGenerator.generate();
    var outId = idGenerator.generate();
    ctx.getMacroIds().put(step.as(), outId);
    Map<String,String> args = Maps.mutable.ofMap(step.args());
    Map<String,String> argRefs = step.argRefs() == null? Map.of() : step.argRefs();
    Map<String, String> ctxArgs = ctx.getArgs() == null? Map.of(): ctx.getArgs();
    for (var argRef : argRefs.entrySet()) {
      args.put(argRef.getKey(), ctxArgs.get(argRef.getValue()));
    }
    InvocationRequest.InvocationRequestBuilder builder = InvocationRequest.builder()
      .invId(invId)
      .outId(outId)
      .main(mainId)
      .body(createReqBody(node, state))
      .cls(mainCls!=null ? mainCls:step.targetCls())
      .args(args)
      .fb(step.function());
    return builder.build();
  }

  JsonBytes createReqBody(DataflowNode node, DataflowState state) {
    JsonBytes resultBody = new JsonBytes(mapper.createObjectNode());
    for (var dmat : node.dmats()) {
      int stepIndex = dmat.node().stepIndex();
      if (stepIndex + 1 < 0)
        continue;
      var stepState = state.stepStates[stepIndex + 1];
      ODataTransformer transformer = dmat.transformer();
      String fromBody = dmat.mapping().fromBody();
      if (fromBody!= null && !fromBody.isEmpty())
        resultBody = transformer.transformMerge(resultBody, stepState.body);
      else if (stepState.obj!=null)
        resultBody = transformer.transformMerge(resultBody, stepState.obj.getData());
    }
    return resultBody;
  }

  boolean ready(DataflowState state,
                DataflowNode node) {
    Set<DataflowNode> require = node.require();
    StepState stepState = state.stepStates[node.stepIndex() + 1];
    if (stepState.sent()) return false;
    if (require.isEmpty()) return true;
    for (DataflowNode prerequisite : require) {
      if (!state.stepStates[prerequisite.stepIndex() + 1].completed)
        return false;
    }
    state.stepStates[node.stepIndex() + 1] = stepState
      .toBuilder()
      .sent(true)
      .build();
    return true;
  }

  @Builder(toBuilder = true)
  record DataflowState(StepState[] stepStates) {
    @Override
    public String toString() {
      return "DataflowState{" +
        "stepStates=" + Arrays.toString(stepStates) +
        '}';
    }
  }

  @Builder(toBuilder = true)
  record StepState(boolean sent,
                   boolean completed,
                   GOObject obj,
                   JsonBytes body,
                   InvocationResponse resp) {
    static StepState NULL = new StepState(false, false,
      null, null, null);
  }
}
