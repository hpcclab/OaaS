package org.hpcclab.oaas.pm.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.impl.factory.Sets;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class PackageValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(PackageValidator.class);
  final FunctionRepository functionRepo;

  public PackageValidator(FunctionRepository functionRepo) {
    this.functionRepo = functionRepo;
  }


  public OPackage validate(OPackage pkg) {
    var classes = pkg.getClasses();
    var functions = pkg.getFunctions();
    var funcMap = functions.stream()
      .map(f -> f.setPkg(pkg.getName()))
      .collect(Collectors.toMap(OFunction::getKey, Function.identity()));
    for (OFunction function : functions) {
      function.setPkg(pkg.getName());
      function.validate();
    }
    for (OClass cls : classes) {
      cls.setPkg(pkg.getName());
      cls.validate();
    }
    validateFunctionBinding(classes, funcMap);
    var macroFunctions = functions.stream()
      .filter(func -> func.getType().equals(FunctionType.MACRO))
      .toList();
    if (macroFunctions.isEmpty())
      return pkg;
    for (OFunction macroFunction : macroFunctions) {
      validateMacro(macroFunction);
    }
    return pkg;
  }

  public void validateFunctionBinding(List<OClass> classes, Map<String, OFunction> functionMap) {
    for (OClass cls : classes) {
      validateFunctionBinding(cls, functionMap);
    }
  }

  public void validateFunctionBinding(OClass cls,
                                           Map<String, OFunction> functionMap) {


    for (FunctionBinding fb : cls.getFunctions()) {
      fb.replaceRelative(cls.getPkg());
      if (fb.getFunction()==null) {
        throw new OaasValidationException("The 'functions[].function' in class must not be null.");
      }
      OFunction function = functionMap.get(fb.getFunction());
      if (function == null)
        function = functionRepo.get(fb.getFunction());
      if (function == null)
        throw FunctionValidationException.format("Can not find function [%s]", fb.getFunction());
      fb.validate(function);
    }
  }

  public void validateMacro(OFunction function) {
    var macro = function.getMacro();
    var steps = macro.getSteps();
    int i = -1;
    Set<String> outSet = Sets.mutable.empty();
    for (var step : steps) {
      i++;
      var target = step.getTarget();
      if (step.getFunction()==null)
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detected null function value."
            .formatted(function.getKey(), i));
      if (target==null)
        throw new FunctionValidationException(
          "Function '%s', step[%d]: Detected null main value."
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
            "Function '%s', step[%d]: main and as values '%s' can not be the same"
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
          "Function '%s', step[%d]: Detect unresolvable main name('%s')"
            .formatted(function.getKey(), i, target)
        );
    }
  }
}
