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
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public abstract class AbsClassControllerRegistry implements ClassControllerRegistry {
  private static final Logger logger = LoggerFactory.getLogger(AbsClassControllerRegistry.class);
  protected final FunctionControllerFactory functionControllerFactory;
  protected final StateManager stateManager;
  protected final IdGenerator idGenerator;
  protected final InvocationQueueProducer invocationQueueProducer;
  protected final MetricFactory metricFactory;
  protected final ProtoMapper protoMapper = new ProtoMapperImpl();
  protected final Map<String, ClassController> classControllerMap = new ConcurrentHashMap<>();


  protected AbsClassControllerRegistry(FunctionControllerFactory functionControllerFactory,
                                       StateManager stateManager,
                                       IdGenerator idGenerator,
                                       InvocationQueueProducer invocationQueueProducer,
                                       MetricFactory metricFactory) {
    this.functionControllerFactory = functionControllerFactory;
    this.stateManager = stateManager;
    this.idGenerator = idGenerator;
    this.invocationQueueProducer = invocationQueueProducer;
    this.metricFactory = metricFactory;
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

    return listCls(outputClsKeys)
      .flatMap(clsMap -> listFn(fnKeys)
        .map(fnMap -> new ClsFnCtx(clsMap, fnMap))
      )
      .map(clsFnCtx -> build(cls, clsFnCtx.classMap(), clsFnCtx.functionMap(), stateManager))
      .invoke(classController -> classControllerMap.put(classController.getCls().getKey(), classController));
  }

  public ClassController getClassController(String clsKey) {
    return classControllerMap.get(clsKey);
  }

  public String printStructure() {
    StringBuilder builder = new StringBuilder();
    for (ClassController classController : classControllerMap.values()) {
      builder.append("- ")
        .append(classController.getCls().getKey())
        .append(": [");
      for (var functionController : classController.getFunctionControllers().values()) {
        builder.append(functionController.getFunctionBinding().getName())
          .append(":")
          .append(functionController.getFunction().getKey())
          .append(",");
      }
      builder.append("]\n");
    }
    return builder.toString();
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

  protected abstract Uni<Map<String, OClass>> listCls(Set<String> keys);

  protected abstract Uni<Map<String, OFunction>> listFn(Set<String> keys);


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
    if (function==null)
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
    logger.debug("build {}", cls.getKey());
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
      createComponent(cls),
      metricFactory
    );
  }
  protected ClassBindingComponent createComponent(OClass cls) {
    return null;
  }
}
