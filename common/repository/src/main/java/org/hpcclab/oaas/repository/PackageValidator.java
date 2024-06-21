package org.hpcclab.oaas.repository;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.FunctionType;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PackageValidator {
  private static final Logger LOGGER = LoggerFactory.getLogger(PackageValidator.class);
  final FunctionRepository functionRepo;

  public PackageValidator(FunctionRepository functionRepo) {
    this.functionRepo = functionRepo;
  }


  public OPackage validate(OPackage pkg) {
    var classes = pkg.getClasses();
    if (classes==null) {
      classes = List.of();
      pkg.setClasses(classes);
    }
    var functions = pkg.getFunctions();
    if (functions==null) {
      functions = List.of();
      pkg.setFunctions(functions);
    }
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
      if (pkg.isDisable()) cls.setDisabled(true);
    }
    validateFunctionBinding(classes, funcMap);
    var macroFunctions = functions.stream()
      .filter(func -> func.getType()==FunctionType.MACRO)
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
      if (function==null)
        function = functionRepo.get(fb.getFunction());
      if (function==null)
        throw FunctionValidationException.format("Can not find function [%s]", fb.getFunction());
      fb.validate(function);
    }
  }

  public void validateMacro(OFunction function) {
    var macro = function.getMacro();
    var error = Dataflows.validate(macro);
    if (error!=null)
      throw FunctionValidationException.format(
        "MacroFunction('%s') %s", function.getKey(), error);
  }
}
