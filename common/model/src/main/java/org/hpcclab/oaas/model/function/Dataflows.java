package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.*;

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
    public Spec cleanNull() {
      return toBuilder()
        .steps(Optional.of(steps).stream().flatMap(Collection::stream).map(Step::cleanNull).toList())
        .build();
    }
  }

  @Builder(toBuilder = true)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  record Step(
    String function,
    String target,
    String targetCls,
    String as,
    List<DataMapping> mappings,
    Map<String,String> args,
    Map<String,String> argRefs
  ) {

    public Step cleanNull() {
      return toBuilder()
        .mappings(mappings == null? List.of(): mappings)
        .args(args == null? Map.of(): args)
        .argRefs(argRefs == null? Map.of(): argRefs)
        .build();
    }
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

    public DataMapping cleanNull() {
      return toBuilder()
        .transforms(transforms == null? List.of(): transforms)
        .build();
    }

    @JsonIgnore
    public String refName() {
      if (fromObj != null && !fromObj.isEmpty())
        return fromObj;
      else
        return fromBody;
    }
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
