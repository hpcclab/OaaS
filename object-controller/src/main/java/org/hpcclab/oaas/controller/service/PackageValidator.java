package org.hpcclab.oaas.controller.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.impl.factory.Sets;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.pkg.OaasPackageContainer;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class PackageValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger( PackageValidator.class );
  @Inject
  FunctionRepository functionRepo;

  public Uni<OaasPackageContainer> validate(OaasPackageContainer pkg) {
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    var funcMap = functions.stream()
      .map(f -> f.setPkg(pkg.getName()))
      .collect(Collectors.toMap(OaasFunction::getKey, Function.identity()));
    for (OaasFunction function : functions) {
      function.setPkg(pkg.getName());
      function.validate();
    }
    for (OaasClass cls : classes) {
      cls.setPkg(pkg.getName());
      cls.validate();
    }
    var uni = validateFunctionBinding(classes, funcMap)
      .replaceWith(pkg);
    var macroFunctions = functions.stream()
      .filter(func -> func.getType().equals(FunctionType.MACRO))
      .toList();
    if (macroFunctions.isEmpty())
      return uni;
    return
      uni.call(() -> Multi.createFrom().iterable(macroFunctions)
        .invoke(this::validateMacro)
        .collect().last());
  }

  public Uni<Void> validateFunctionBinding(List<OaasClass> classes, Map<String, OaasFunction> functionMap) {
    return Multi.createFrom().iterable(classes)
      .call(cls -> validateFunctionBinding(cls, functionMap))
      .collect()
      .last()
      .replaceWithVoid();
  }

  public Uni<Void> validateFunctionBinding(OaasClass cls,
                                           Map<String, OaasFunction> functionMap) {
    return Multi.createFrom().iterable(cls.getFunctions())
      .map(binding -> binding.replaceRelative(cls.getPkg()))
      .call(binding -> Uni.createFrom()
        .item(functionMap.get(binding.getFunction()))
        .onItem().ifNull()
        .switchTo(() -> functionRepo.getWithoutCacheAsync(binding.getFunction()))
        .onItem().ifNull().failWith(() -> new FunctionValidationException("Can not find function [%s]".formatted(binding.getFunction())))
        .invoke(func -> binding.validate(func)))
      .collect().last()
      .replaceWithVoid();
  }

  public void validateMacro(OaasFunction function) {
    var macro = function.getMacro();
    var steps = macro.getSteps();
    int i = -1;
    Set<String> outSet = Sets.mutable.empty();
    for (var step : steps) {
      i++;
      var target = step.getTarget();
      if (step.getFunction() == null)
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detected null function value."
            .formatted(function.getKey(), i));
      if (target==null)
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detected null target value."
            .formatted(function.getKey(), i));

      if (step.getAs()!=null) {
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
    }
  }
}
