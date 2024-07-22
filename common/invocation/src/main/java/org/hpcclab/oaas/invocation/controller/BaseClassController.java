package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.invocation.metrics.MetricFactory;
import org.hpcclab.oaas.invocation.state.DeleteStateOperation;
import org.hpcclab.oaas.invocation.state.SimpleStateOperation;
import org.hpcclab.oaas.invocation.state.StateManager;
import org.hpcclab.oaas.invocation.state.StateOperation;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public class BaseClassController implements ClassController {
  private static final Logger logger = LoggerFactory.getLogger( BaseClassController.class );

  final OClass cls;
  final StateManager stateManager;
  final InvocationQueueProducer producer;
  final IdGenerator idGenerator;
  final MetricFactory metricFactory;
  final InvocationChainProcessor chainProcessor;
  Map<String, FunctionController> functionMap;

  public BaseClassController(OClass cls,
                             Map<String, FunctionController> functionMap,
                             StateManager stateManager,
                             IdGenerator idGenerator,
                             InvocationQueueProducer producer,
                             MetricFactory metricFactory, InvocationChainProcessor chainProcessor) {
    this.cls = cls;
    this.functionMap = functionMap;
    this.stateManager = stateManager;
    this.idGenerator = idGenerator;
    this.producer = producer;
    this.metricFactory = metricFactory;
    this.chainProcessor = chainProcessor;
  }

  @Override
  public Uni<InvocationCtx> invoke(InvocationCtx context) {
    var req = context.getRequest();
    if (req.fb()==null || req.fb().isEmpty())
      return Uni.createFrom().item(context);
    var fn = functionMap.get(req.fb());
    if (fn==null)
      return Uni.createFrom().failure(InvocationException.notFoundFnInCls(req.fb(), cls.getKey()));
    logger.debug("invoke fb={} to {} in cls {} (out={})", req.fb(), req.main(), req.cls(), req.outId());
    return fn.invoke(context)
      .flatMap(this::handleStateOperations)
      .call(chainProcessor::handle);
  }

  @Override
  public ValidationContext validate(InvocationRequest request) {
    if (!request.fb().isEmpty()) {
      var fn = functionMap.get(request.fb());
      if (fn==null) throw InvocationException.notFoundFnInCls(request.fb(), cls.getKey());
      var req = request.toBuilder()
        .invId(idGenerator.generate())
        .partKey(request.main())
        .immutable(fn.getFunctionBinding().isImmutable());
      if (fn.getFunctionBinding().getOutputCls()!=null) {
        req.outId(idGenerator.generate());
      }
      if (fn.getFunction().getType()==FunctionType.MACRO) {
        var dataflow = fn.getFunction().getMacro();
        var map = Lists.fixedSize.ofAll(dataflow.steps())
          .select(step -> step.as()!=null)
          .collect(step -> Map.entry(step.as(), idGenerator.generate()))
          .toMap(Map.Entry::getKey, Map.Entry::getValue);
        if (dataflow.output()!=null)
          req.outId(map.get(dataflow.output()));
      }
      return new ValidationContext(
        req.build(), cls, fn.getFunctionBinding().getOutputCls(),
        fn.getFunction(), fn.getFunctionBinding());
    }
    return new ValidationContext(
      request, cls, null, null, null);
  }

  @Override
  public OClass getCls() {
    return cls;
  }

  @Override
  public FunctionController getFunctionController(String fb) {
    return functionMap.get(fb);
  }

  @Override
  public Map<String, FunctionController> getFunctionControllers() {
    return functionMap;
  }

  public Uni<InvocationCtx> handleStateOperations(InvocationCtx context) {
    var ops = context.getStateOperations();
    if (ops.isEmpty()) return Uni.createFrom().item(context);
    if (ops.size()==1) {
      return handleStateOperation(ops.getFirst()).replaceWith(context);
    } else {
      return Multi.createFrom().iterable(ops)
        .onItem().transformToUniAndConcatenate(op -> handleStateOperation(op)
          .replaceWith(context))
        .collect().last();
    }
  }

  Uni<Void> handleStateOperation(StateOperation stateOperation) {
    return switch (stateOperation) {
      case SimpleStateOperation sso -> stateManager.applySimple(sso);
      case DeleteStateOperation dso -> stateManager.applyDelete(dso);
      default -> throw new IllegalStateException("Unexpected value:" + stateOperation);
    };
  }

  @Override
  public void updateFunctionController(String fnKey,
                                       UnaryOperator<FunctionController> updater) {
    var controllerToUpdate = functionMap.values().stream()
      .filter(con -> con.getFunction().getKey().equals(fnKey))
      .toList();
    var newMap = Maps.mutable.ofMap(functionMap);
    for (FunctionController fc : controllerToUpdate) {
      newMap.put(fc.getFunctionBinding().getName(), updater.apply(fc));
    }
    functionMap = newMap;
  }

  @Override
  public Uni<Void> enqueue(InvocationRequest req) {
    return producer.offer(req);
  }
}
