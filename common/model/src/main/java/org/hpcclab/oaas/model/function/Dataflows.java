package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;
import java.util.Set;

/**
 * @author Pawissanutt
 */
public interface Dataflows {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Builder(toBuilder = true)
  record Spec (
    List<Dataflows.Step> steps,
    List<DataMapping> respBody,
    String output
  ){
  }

  @Builder(toBuilder = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Step(
    String function,
    String target,
    String targetCls,
    String as,
    List<DataMapping> mappings,
    DSMap args,
    DSMap argRefs
  ) {
  }


  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Builder(toBuilder = true)
  record DataMapping(
    String fromObj,
    String fromBody,
    List<Transformation> transforms,
    boolean failOnError,
    boolean mapAll
  ) {

  }

  record Transformation(String path, String inject){}

  static String validate(Dataflows.Spec spec) {
    var steps = spec.steps();
    int i = -1;
    Set<String> outSet = org.eclipse.collections.impl.factory.Sets.mutable.empty();
    for (var step : steps) {
      i++;
      var target = step.target();
      if (step.function()==null)
        return "step[%d]: Detected null function value.".formatted(i);
      if (target==null)
        return "step[%d]: Detected null main value.".formatted(i);

      if (step.as()!=null) {
        if (outSet.contains(step.as())) {
          return "step[%d]: Detect duplication of as value of '%s'".formatted(i,step.as());
        }
        if (step.as().equals(step.target())) {
          if (outSet.contains(step.as())) {
            return "step[%d]: main and as values '%s' can not be the same".formatted(i, step.as());
          }
        }
        outSet.add(step.as());
      }
      if (target.startsWith("@") || target.startsWith("#"))
        continue;
      var paths = target.split("\\.");
      if (!outSet.contains(paths[0]))
        return "step[%d]:Detect unresolvable main name('%s')".formatted(i, target);
    }
    return null;
  }
}
