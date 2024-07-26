package org.hpcclab.oaas.invocation.dataflow;

import lombok.Builder;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;

import java.util.Arrays;

/**
 * @author Pawissanutt
 */
public record DataflowState(StepState[] stepStates) {
  @Override
  public String toString() {
    return "DataflowState{" +
      "stepStates=" + Arrays.toString(stepStates) +
      '}';
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
