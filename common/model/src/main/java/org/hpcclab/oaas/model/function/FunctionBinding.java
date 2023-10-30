package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionBinding {
  @ProtoField(1)
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  @ProtoField(2)
  String function;
  @ProtoField(3)
  String name;
  @ProtoField(value = 5)
  DSMap defaultArgs;
  @ProtoField(6)
  String description;
  @ProtoField(7)
  String outputCls;
  @ProtoField(value = 8, defaultValue = "false")
  boolean forceImmutable = false;
  @ProtoField(value = 9, defaultValue = "false")
  boolean noMain;
  @ProtoField(10)
  List<String> inputTypes;


  public FunctionBinding() {
  }

  @ProtoFactory

  public FunctionBinding(FunctionAccessModifier access, String function, String name, DSMap defaultArgs, String description, String outputCls, boolean forceImmutable, boolean noMain, List<String> inputTypes) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.defaultArgs = defaultArgs;
    this.description = description;
    this.outputCls = outputCls;
    this.forceImmutable = forceImmutable;
    this.noMain = noMain;
    this.inputTypes = inputTypes;
  }

  public void validate() {
    if (function==null) {
      throw new OaasValidationException("The 'functions[].function' in class must not be null.");
    }
    if (name==null) {
      var i = function.lastIndexOf('.');
      if (i < 0) name = function;
      else name = function.substring(i + 1);
    }
  }

  public void validate(OaasFunction oaasFunction) {
    if (outputCls==null) {
      outputCls = oaasFunction.getOutputCls();
    } else if (outputCls.equalsIgnoreCase("none") ||
      outputCls.equalsIgnoreCase("void")) {
      outputCls = null;
    }
  }

  public FunctionBinding replaceRelative(String pkgName) {
    if (function!=null && function.startsWith("."))
      function = pkgName + function;
    if (outputCls!=null && outputCls.startsWith("."))
      outputCls = pkgName + outputCls;
    return this;
  }
}
