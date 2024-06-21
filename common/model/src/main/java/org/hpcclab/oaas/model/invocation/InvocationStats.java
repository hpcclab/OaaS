package org.hpcclab.oaas.model.invocation;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record InvocationStats(
  long queTs,
  long smtTs,
  long cptTs
) {
}
