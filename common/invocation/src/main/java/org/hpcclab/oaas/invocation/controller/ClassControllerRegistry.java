package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ClassService;
import org.hpcclab.oaas.proto.FunctionService;
import org.hpcclab.oaas.proto.MultiKeyQuery;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public class ClassControllerRegistry {
  private static final Logger logger = LoggerFactory.getLogger( ClassControllerRegistry.class );
  final ClassService classService;
  final FunctionService functionService;
  final FunctionControllerFactory functionControllerFactory;
  final StateManager stateManager;
  final IdGenerator idGenerator;
  final InvocationQueueProducer invocationQueueProducer;
  final MetricFactory metricFactory;

  ProtoMapper protoMapper = new ProtoMapperImpl();
  Map<String, ClassController> classControllerMap;

  public ClassControllerRegistry(ClassService classService,
                                 FunctionService functionService,
                                 FunctionControllerFactory functionControllerFactory,
                                 StateManager stateManager,
                                 IdGenerator idGenerator,
                                 InvocationQueueProducer invocationQueueProducer, MetricFactory metricFactory) {
    this.classService = classService;
    this.functionService = functionService;
    this.functionControllerFactory = functionControllerFactory;
    this.stateManager = stateManager;
    this.idGenerator = idGenerator;
    this.invocationQueueProducer = invocationQueueProducer;
      this.metricFactory = metricFactory;
      this.classControllerMap = new ConcurrentHashMap<>();
  }

  public Uni<ClassController> registerOrUpdate(ProtoOClass cls) {
    return registerOrUpdate(protoMapper.fromProto(cls));
  }
  public Uni<ClassController> registerOrUpdate(OClass cls) {
    logger.info("registerOrUpdate class({})", cls.getKey());
    var outputClsKeys = cls.getFunctions()
      .stream()
      .map(FunctionBinding::getOutputCls)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    var fnKeys = cls.getFunctions()
      .stream()
      .map(FunctionBinding::getFunction)
      .collect(Collectors.toSet());
    record ClsFnCtx(Map<String, OClass> classMap, Map<String, OFunction> functionMap) {
    }

    return classService.select(MultiKeyQuery.newBuilder().addAllKey(outputClsKeys).build())
      .map(protoMapper::fromProto)
      .collect()
      .asMap(OClass::getKey)
      .flatMap(clsMap ->
        functionService.select(MultiKeyQuery.newBuilder().addAllKey(fnKeys).build())
          .map(protoMapper::fromProto)
          .collect().asMap(OFunction::getKey)
          .map(fnMap -> new ClsFnCtx(clsMap, fnMap))
      )
      .map(clsFnCtx -> build(cls, clsFnCtx.classMap(), clsFnCtx.functionMap(), stateManager))
      .invoke(classController -> classControllerMap.put(classController.getCls().getKey(), classController));
  }

  public void updateFunction(OFunction function) {
    for (ClassController controller : classControllerMap.values()) {
      controller.updateFunctionController(function.getKey(),
        fc -> buildFnController(
          fc.getFunctionBinding(),
          function,
          controller.getCls(),
          controller.getCls()
        ));
    }
  }

  private FunctionController buildFnController(FunctionBinding functionBinding,
                                       Map<String, OFunction> functionMap,
                                       OClass cls,
                                       Map<String, OClass> ctxClsMap) {
    var function = functionMap.get(functionBinding.getFunction());
    var outputCls = functionBinding.getOutputCls()==null ? null:
      ctxClsMap.get(functionBinding.getOutputCls());
    return buildFnController(functionBinding, function, cls, outputCls);
  }

  private FunctionController buildFnController(FunctionBinding functionBinding,
                                       OFunction function,
                                       OClass cls,
                                       OClass outputCls) {
    if (function == null)
      throw StdOaasException.format("Cannot load OFunction(%s) for OClass(%s)",
        functionBinding.getFunction(),
        cls.getKey());
    var controller = functionControllerFactory.create(function);
    controller.bind(metricFactory, functionBinding, function, cls, outputCls);
    return controller;
  }


  private ClassController build(OClass cls,
                        Map<String, OClass> ctxClsMap,
                        Map<String, OFunction> fnMap,
                        StateManager stateManager) {
    Map<String, FunctionController> fbToFnMap = cls.getFunctions()
      .stream()
      .map(fb -> Map.entry(fb.getName(), buildFnController(fb, fnMap, cls, ctxClsMap)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new BaseClassController(
      cls,
      fbToFnMap,
      stateManager,
      idGenerator,
      invocationQueueProducer,
      metricFactory
    );
  }

  public ClassController getClassController(String clsKey) {
    return classControllerMap.get(clsKey);
  }

  public String printStructure(){
    StringBuilder builder = new StringBuilder();
    for (ClassController classController : classControllerMap.values()) {
      builder.append("- ")
        .append(classController.getCls().getKey())
        .append(": [");
      for (var functionController : classController.getFunctionControllers().values()) {
        builder.append("{")
          .append(functionController.getFunctionBinding().getName())
          .append(":")
          .append(functionController.getFunction().getKey())
          .append("},");
      }
      builder.append("]\n");
    }
    return builder.toString();
  }
}
