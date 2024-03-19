package org.hpcclab.oaas.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.controller.fn.*;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFunctionController;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;

public class MockFunctionControllerFactory implements FunctionControllerFactory {
  IdGenerator idGenerator = new TsidGenerator();
  ObjectMapper mapper = new ObjectMapper();
  OffLoader offLoader = new MockOffLoader();
  ContentUrlGenerator contentUrlGenerator = new SaContentUrlGenerator("http://localhost:8090");
  DataUrlAllocator dataUrlAllocator = new MockDataUrlAllocator();
  @Override
  public FunctionController create(OFunction function) {
    return switch (function.getType()) {
      case TASK, IM_TASK -> new TaskFunctionController(idGenerator, mapper, offLoader, contentUrlGenerator);
      case LOGICAL -> createLogical(function);
      case MACRO -> new MacroFunctionController(idGenerator, mapper);
      default -> throw new IllegalArgumentException("function %s not supported".formatted(function.getKey()));
    };
  }

  LogicalFunctionController createLogical(OFunction function) {
    if (function.getKey().equals("builtin.logical.new")) {
      return new NewFunctionController(idGenerator, mapper, dataUrlAllocator);
    }
    throw new IllegalArgumentException();
  }
}
