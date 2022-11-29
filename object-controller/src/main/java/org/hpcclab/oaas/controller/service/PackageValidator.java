package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.impl.factory.Sets;
import org.hpcclab.oaas.model.OaasPackageContainer;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;

import javax.enterprise.context.ApplicationScoped;
import java.util.Set;

@ApplicationScoped
public class PackageValidator {

  public Uni<OaasPackageContainer> validate(OaasPackageContainer pkg) {
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    for (OaasClass cls : classes) {
      cls.setPkg(pkg.getName());
      cls.validate();
    }
    for (OaasFunction function : functions) {
      function.setPkg(pkg.getName());
      function.validate();
    }
    var macroFunctions = functions.stream()
      .filter(func -> func.getType().equals(FunctionType.MACRO))
      .toList();
    if (macroFunctions.isEmpty())
      return Uni.createFrom().item(pkg);
    return Multi.createFrom().iterable(macroFunctions)
      .invoke(this::validateMacro)
      .collect().last()
      .replaceWith(pkg);
  }

  public void validateMacro(OaasFunction function) {
    var macro = function.getMacro();
    var steps = macro.getSteps();
    int i = -1;
    Set<String> outSet = Sets.mutable.empty();
    for (var step : steps) {
      i++;
      var target = step.getTarget();
      if (target == null)
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detected null target value in ."
            .formatted(function.getKey(), i));

      if (step.getAs() != null) {
        if (outSet.contains(step.getAs())) {
          throw new FunctionValidationException(
            "Function '%s', step[%d]: Detect duplication of as value of '%s'"
              .formatted(function.getKey(), i, step.getAs())
          );
        }
        if (step.getAs().equals(step.getTarget())) {
          throw new FunctionValidationException(
            "Function '%s', step[%d]: target and as values '%s' can not be the same"
              .formatted(function.getKey(), i, step.getAs())
          );
        }
        outSet.add(step.getAs());
      }
      if (target.startsWith("$") || target.startsWith("#"))
        continue;
      var paths = target.split("\\.");
      if (!outSet.contains(paths[0]))
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detect unresolvable target name('%s')"
            .formatted(function.getKey(), i, target)
        );
//
    }
  }
}
