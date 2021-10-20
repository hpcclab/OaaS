package org.hpcclab.msc.object.handler;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.apache.commons.lang3.math.NumberUtils;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasWorkflow;
import org.hpcclab.msc.object.entity.object.OaasCompoundMember;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.object.OaasObjectOrigin;
import org.hpcclab.msc.object.model.FunctionExecContext;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class MacroFunctionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger( MacroFunctionHandler.class );

  @Inject
  OaasObjectRepository objectRepo;
  @Inject
  FunctionRouter router;

  public void validate(FunctionExecContext context) {
    if (context.getMain().getType()!=OaasObject.ObjectType.COMPOUND)
      throw new NoStackException("Object must be COMPOUND").setCode(400);
    if (context.getFunction().getType()!=OaasFunction.FuncType.MACRO)
      throw new NoStackException("Function must be MACRO").setCode(400);
  }

  private OaasObject resolveTarget(FunctionExecContext context, String value) {
    if (NumberUtils.isDigits(value)) {
      var i = Integer.parseInt(value);
      return context.getAdditionalInputs().get(i);
    }
    return context.getMain().getMembers()
      .stream()
      .filter(cm -> cm.getName().equals(value))
      .findAny().orElseThrow(() -> new NoStackException("Can not resolve '"+value+"'"))
      .getObject();
  }

  public Uni<OaasObject> call(FunctionExecContext context) {
    validate(context);

    var func = context.getFunction();
    var output = OaasObject.createFromClasses(context.getFunction().getOutputCls());
    output.setOrigin(new OaasObjectOrigin(context));

    return execWorkflow(context, func.getMacro())
      .flatMap(wfResults -> {
        var mem = func.getMacro().getExports()
          .stream()
          .map(export -> new OaasCompoundMember(export.getAs(),wfResults.get(export.getFrom())))
          .toList();
        output.setMembers(mem);
        return objectRepo.persistAndFlush(output);
      });
  }

  private Uni<Map<String, OaasObject>> execWorkflow(FunctionExecContext context,
                                       OaasWorkflow workflow) {
    var map = new HashMap<String, OaasObject>();
    return Multi.createFrom().iterable(workflow.getSteps())
      .flatMap(step -> {
        var target = resolveTarget(context, step.getTarget());
        var function = target.getFunctions()
          .stream()
          .filter(fb -> fb.getFunction().getName().equals(step.getFuncName()))
          .findAny().orElseThrow();
        //TODO
      })
      .collect().last();
  }
}
