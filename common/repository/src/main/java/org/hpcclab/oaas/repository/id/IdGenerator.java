package org.hpcclab.oaas.repository.id;

import org.hpcclab.oaas.model.invocation.InvocationContext;

public interface IdGenerator {
  default String generate(InvocationContext context) {
    return generate();
  }
  String generate();
}
