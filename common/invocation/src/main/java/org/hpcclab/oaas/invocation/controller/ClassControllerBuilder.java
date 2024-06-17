package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.ErrorFnController;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.invocation.controller.fn.FunctionControllerFactory;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.proto.ProtoOClass;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
public abstract class ClassControllerBuilder {
  private static final Logger logger = LoggerFactory.getLogger(ClassControllerBuilder.class);

  protected final FunctionControllerFactory functionControllerFactory;
  protected final StateManager stateManager;
  protected final IdGenerator idGenerator;
  protected final InvocationQueueProducer invocationQueueProducer;
  protected final MetricFactory metricFactory;
  protected final ProtoMapper protoMapper = new ProtoMapperImpl();

  protected ClassControllerBuilder(FunctionControllerFactory functionControllerFactory, StateManager stateManager, IdGenerator idGenerator, InvocationQueueProducer invocationQueueProducer, MetricFactory metricFactory) {
    this.functionControllerFactory = functionControllerFactory;
    this.stateManager = stateManager;
    this.idGenerator = idGenerator;
    this.invocationQueueProducer = invocationQueueProducer;
    this.metricFactory = metricFactory;
  }

  public Uni<ClassController> build(ProtoOClass cls) {
    return build(protoMapper.fromProto(cls));
  }

  public Uni<ClassController> build(OClass cls) {
    logger.info("registerOrUpdate class({})", cls.getKey());
    var outputClsKeys = cls
      .getResolved()
      .getFunctions()
      .values().stream()
      .map(FunctionBinding::getOutputCls)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    var fnKeys = cls.getResolved()
      .getFunctions()
      .values()
      .stream()
      .map(FunctionBinding::getFunction)
      .collect(Collectors.toSet());
    record ClsFnCtx(Map<String, OClass> classMap, Map<String, OFunction> functionMap) {
    }

    return listCls(outputClsKeys)
      .flatMap(clsMap -> listFn(fnKeys)
        .map(fnMap -> new ClsFnCtx(clsMap, fnMap))
      )
      .map(clsFnCtx -> build(cls, clsFnCtx.classMap(), clsFnCtx.functionMap(), stateManager));
  }

  protected abstract Uni<Map<String, OClass>> listCls(Set<String> keys);

  protected abstract Uni<Map<String, OFunction>> listFn(Set<String> keys);

  private ClassController build(OClass cls,
                                Map<String, OClass> ctxClsMap,
                                Map<String, OFunction> fnMap,
                                StateManager stateManager) {
    logger.debug("build {}", cls.getKey());
    Map<String, FunctionController> fbToFnMap = cls.getResolved()
      .getFunctions()
      .entrySet()
      .stream()
      .map(entry -> Map.entry(entry.getKey(), buildFnController(entry.getValue(), fnMap, cls, ctxClsMap)))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return new BaseClassController(
      cls,
      fbToFnMap,
      stateManager,
      idGenerator,
      invocationQueueProducer,
      createComponent(cls),
      metricFactory,
      new InvocationChainProcessor(invocationQueueProducer)
    );
  }

  public UnaryOperator<FunctionController> createUpdator(OFunction fn) {
    return fc -> buildFnController(
      fc.getFunctionBinding(),
      fn,
      fc.getCls(),
      fc.getOutputCls()
    );
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
    if (function==null) {
      var errorMsg = "Cannot load OFunction(%s) for OClass(%s)"
        .formatted(functionBinding.getFunction(), cls.getKey());
      logger.error(errorMsg);
      ErrorFnController controller = new ErrorFnController(errorMsg);
      var func = new OFunction().setKey(functionBinding.getFunction());
      controller.bind(metricFactory, functionBinding, func, cls, outputCls);
      return controller;
    }
    FunctionController controller;
    try {
      controller = functionControllerFactory.create(function);
      controller.bind(metricFactory, functionBinding, function, cls, outputCls);
      return controller;
    } catch (Exception e) {
      logger.error("Fail to initiate the function controller for {}:{}",
        cls.getKey(), functionBinding.getName(), e);
      controller = new ErrorFnController(e.getMessage());
      controller.bind(metricFactory, functionBinding, function, cls, outputCls);
      return controller;
    }

  }

  protected ClassBindingComponent createComponent(OClass cls) {
    return null;
  }

}
