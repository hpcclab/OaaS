package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;

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

  record Export(String from, String as) {}

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

}
