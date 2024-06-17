package org.hpcclab.oaas.invocation.dataflow;

import org.hpcclab.oaas.model.function.Dataflows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


/**
 * @author Pawissanutt
 */
class DataflowSemanticTest {


  @Test
  void test() {
    var steps = List.of(
      Dataflows.Step.builder().target("@").as("a").build(),
      Dataflows.Step.builder().target("a").as("b").build(),
      Dataflows.Step.builder().target("b").as("c").build()
    );
    Dataflows.Spec spec = Dataflows.Spec.builder()
      .steps(steps)
      .output("c")
      .build();
    DataflowSemantic semantic = DataflowSemantic.construct(spec);

    assertThat(semantic.getRootNode().next())
      .size().isEqualTo(1);
    assertThat(semantic.getEndNode().require())
      .size().isEqualTo(1);
    assertThat(semantic.getAllNode())
      .size().isEqualTo(3);
    System.out.println(semantic.getEndNode());
  }
}
