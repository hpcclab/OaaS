package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * @author Pawissanutt
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record DataMapperDefinition(
  String target,
  String inject) {

}
