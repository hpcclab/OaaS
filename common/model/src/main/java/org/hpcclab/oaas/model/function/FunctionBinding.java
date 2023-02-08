package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.exception.OaasValidationException;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashMap;
import java.util.Map;
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
  @ProtoField(4)
  Set<String> forwardRecords;
  @ProtoField(value = 5, javaType = HashMap.class)
  Map<String, String> defaultArgs;
  @ProtoField(6)
  String description;
  @ProtoField(7)
  String outputCls;
  @ProtoField(value = 8, defaultValue = "false")
  boolean forceImmutable = false;


  public FunctionBinding() {
  }

  public FunctionBinding(FunctionAccessModifier access, String function, String name, Set<String> forwardRecords, Map<String, String> defaultArgs, String description, String outputCls,
                         boolean forceImmutable) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.forwardRecords = forwardRecords;
    this.defaultArgs = defaultArgs;
    this.description = description;
    this.outputCls = outputCls;
    this.forceImmutable = forceImmutable;
  }

  @ProtoFactory
  public FunctionBinding(FunctionAccessModifier access,
                         String function,
                         String name,
                         Set<String> forwardRecords,
                         HashMap<String, String> defaultArgs,
                         String description,
                         String outputCls,
                         boolean forceImmutable) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.forwardRecords = forwardRecords;
    this.defaultArgs = defaultArgs;
    this.description = description;
    this.outputCls = outputCls;
    this.forceImmutable = forceImmutable;
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
