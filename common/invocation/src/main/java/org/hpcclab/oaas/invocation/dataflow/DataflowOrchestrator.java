package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic.DataflowNode;
import org.hpcclab.oaas.invocation.dataflow.DataflowState.StepState;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                            DataflowNode preNode,
                            DataflowState state) {
    Set<DataflowNode> next = preNode.next();
    return Multi.createFrom().iterable(next)
      .filter(n -> isReady(state, n))
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
    StepState stepState = state.stepStates()[node.stepIndex()];
    state.stepStates()[node.stepIndex()] = stepState
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
    if (mainRefStepIndex >= 0) {
      var obj = state.stepStates()[mainRefStepIndex].obj();
      ctx.setOutput(obj);
    }
    var body = createReqBody(endNode, ctx, state);
    ctx.setRespBody(body.getNode());
    return ctx;
  }

  InvocationRequest createReq(DataflowSemantic.DataflowNode node,
                              InvocationCtx ctx,
                              DataflowState state) {
    Dataflows.Step step = node.step();
    String mainId = null;
    String mainCls = null;
    if (node.mainRefStepIndex() >= 0) {
      StepState stepState = state.stepStates()[node.mainRefStepIndex()];
      if (stepState.obj()!=null) {
        mainId = stepState.obj().getKey();
        mainCls = stepState.obj().getMeta().getCls();
      }
    }
    var invId = idGenerator.generate();
    var outId = ctx.getMacroIds().computeIfAbsent(step.as(), k -> idGenerator.generate());
    Map<String, String> args = node.argsReplacer().getReplaceVal(ctx, state);
    JsonBytes reqBody = createReqBody(node, ctx, state);
    InvocationRequest.InvocationRequestBuilder builder = InvocationRequest.builder()
      .invId(invId)
      .outId(outId)
      .main(mainId)
      .body(reqBody)
      .cls(mainCls!=null ? mainCls:step.targetCls())
      .args(args)
      .fb(step.function());
    return builder.build();
  }

  JsonBytes createReqBody(DataflowNode node,
                          InvocationCtx ctx,
                          DataflowState state) {
    TemplateProcessor.Replacer<JsonNode> jsonNodeReplacer = node.bodyReplacer();
    if (jsonNodeReplacer == null) return JsonBytes.EMPTY;
    return new JsonBytes(jsonNodeReplacer
      .getReplaceVal(ctx, state));
  }

  boolean isReady(DataflowState state,
                  DataflowNode node) {
    Set<DataflowNode> require = node.require();
    StepState stepState = state.stepStates()[node.stepIndex()];
    if (stepState.sent()) return false;
    if (require.isEmpty()) return true;
    for (DataflowNode prerequisite : require) {
      if (!state.stepStates()[prerequisite.stepIndex()].completed())
        return false;
    }
    state.stepStates()[node.stepIndex()] = stepState
      .toBuilder()
      .sent(true)
      .build();
    return true;
  }
}
