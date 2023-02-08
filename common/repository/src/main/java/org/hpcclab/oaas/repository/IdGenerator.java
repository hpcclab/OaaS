package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;

import java.util.UUID;

public interface IdGenerator {
  default String generate(FunctionExecContext context) {
    return generate();
  }
  default String generate(ObjectConstructRequest request){
    return generate();
  }
  String generate();
}
