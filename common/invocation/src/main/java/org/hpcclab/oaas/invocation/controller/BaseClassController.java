package org.hpcclab.oaas.invocation.controller;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.InvocationQueueProducer;
import org.hpcclab.oaas.invocation.controller.fn.FunctionController;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * @author Pawissanutt
 */
public class BaseClassController implements ClassController {

  final OClass cls;
  final StateManager stateManager;
  final InvocationQueueProducer producer;
  final IdGenerator idGenerator;
  Map<String, FunctionController> functionMap;

  public BaseClassController(OClass cls,
                             Map<String, FunctionController> functionMap,
                             StateManager stateManager,
                             IdGenerator idGenerator,
                             InvocationQueueProducer producer) {
    this.cls = cls;
    this.stateManager = stateManager;
    this.idGenerator = idGenerator;
    this.producer = producer;
    this.functionMap = functionMap;
  }

  @Override
  public Uni<InvocationCtx> invoke(InvocationCtx context) {
    var req = context.getRequest();
    var fn = functionMap.get(req.fb());
    if (fn==null)
      return Uni.createFrom().failure(InvocationException.notFoundFnInCls(req.fb(), cls.getKey()));
    return fn.invoke(context)
      .flatMap(this::handleStateOperations)
      .call(ctx -> producer.offer(ctx.reqToProduce));
  }

  @Override
  public MinimalValidationContext validate(ObjectAccessLanguage oal) {
    var fn = functionMap.get(oal.getFb());
    if (fn==null) throw InvocationException.notFoundFnInCls(oal.getFb(), cls.getKey());
    var req = oal.toRequest()
      .immutable(fn.getFunctionBinding().isForceImmutable());
    if (fn.getFunction().getType()==FunctionType.MACRO) {
      req.macro(true);
      var dataflow = fn.getFunction().getMacro();
      var map = Lists.fixedSize.ofAll(dataflow.getSteps())
        .select(step -> step.getAs()!=null)
        .collect(step -> Map.entry(step.getAs(), idGenerator.generate()))
        .toMap(Map.Entry::getKey, Map.Entry::getValue);
      req.macroIds(DSMap.wrap(map));
      if (dataflow.getExport()!=null)
        req.outId(map.get(dataflow.getExport()));
    }
    return new MinimalValidationContext(req.build(), cls, fn.getFunction(), fn.getFunctionBinding());
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
