package org.hpcclab.oaas.repository.id;

import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;

public interface IdGenerator {
  default String generate(InvocationContext context) {
    return generate();
  }
  default String generate(ObjectConstructRequest request){
    return generate();
  }
  String generate();
}
