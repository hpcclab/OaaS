package org.hpcclab.oaas.invocation.dataflow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import lombok.Builder;
import org.eclipse.collections.api.set.MutableSet;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.dataflow.DataflowSemantic.DataflowNode;
import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pawissanutt
 */
public class DataflowOrchestrator {
  private static final Logger logger = LoggerFactory.getLogger( DataflowOrchestrator.class );
  final LocationAwareInvocationForwarder invocationForwarder;
  final IdGenerator idGenerator;

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
      .replaceWith(ctx);
  }

  Uni<DataflowState> branch(InvocationCtx ctx,
                            DataflowNode node,
                            DataflowState state) {
    MutableSet<DataflowNode> next = node.getNext();
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
    StepState stepState = state.stepStates[node.stepIndex + 1];
    state.stepStates[node.stepIndex + 1] = stepState
      .toBuilder()
      .completed(true)
      .sent(true)
      .obj(resp.output())
      .content(resp.body())
      .resp(resp)
      .build();
    return branch(ctx, node, state);
  }

  InvocationRequest createReq(DataflowSemantic.DataflowNode node,
                              InvocationCtx ctx,
                              DataflowState state) {
    DataflowStep step = node.getStep();
    String mainId = null;
    String mainCls = null;
    if (node.mainRefStepIndex >= -1) {
      StepState stepState = state.stepStates()[node.mainRefStepIndex + 1];
      if (stepState.obj()!=null) {
        mainId = stepState.obj().getKey();
        mainCls = stepState.obj().getCls();
      }
    }
    var invId = idGenerator.generate();
    var outId = idGenerator.generate();
    ctx.getMacroIds().put(step.as(), outId);
    InvocationRequest.InvocationRequestBuilder builder = InvocationRequest.builder()
      .invId(invId)
      .outId(outId)
      .main(mainId)
      .cls(mainCls!=null ? mainCls:step.targetCls())
      .args(step.args())
      .fb(step.function());
    return builder.build();
  }

  boolean ready(DataflowState state,
                DataflowNode node) {
    MutableSet<DataflowNode> require = node.getRequire();
    StepState stepState = state.stepStates[node.stepIndex + 1];
    if (stepState.sent()) return false;
    if (require.isEmpty()) return true;
    for (DataflowNode prerequisite : require) {
      if (!state.stepStates[prerequisite.getStepIndex() + 1].completed)
        return false;
    }
    state.stepStates[node.stepIndex + 1] = stepState
      .toBuilder()
      .sent(true)
      .build();
    return true;
  }

  @Builder(toBuilder = true)
  record DataflowState(StepState[] stepStates) {
  }

  @Builder(toBuilder = true)
  record StepState(boolean sent,
                   boolean completed,
                   OObject obj,
                   ObjectNode content,
                   InvocationResponse resp) {
    static StepState NULL = new StepState(false, false,
      null, null, null);
  }
}
