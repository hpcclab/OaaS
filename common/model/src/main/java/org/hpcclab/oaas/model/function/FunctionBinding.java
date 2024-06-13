package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;
import org.infinispan.protostream.annotations.ProtoFactory;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionBinding {
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  String function;
  String name;
  DSMap defaultArgs;
  String description;
  String outputCls;
  @JsonAlias("forceImmutable")
  boolean immutable;
  boolean noMain;
  boolean noOutput;
  public FunctionBinding() {
  }

  @ProtoFactory
  public FunctionBinding(FunctionAccessModifier access,
                         String function,
                         String name,
                         DSMap defaultArgs,
                         String description,
                         String outputCls,
                         boolean immutable,
                         boolean noMain) {
    this.access = access;
    this.function = function;
    this.name = name;
    this.defaultArgs = defaultArgs;
    this.description = description;
    this.outputCls = outputCls;
    this.immutable = immutable;
    this.noMain = noMain;
  }


  public void validate(OFunction fn) {
    if (name==null) {
      var i = function.lastIndexOf('.');
      if (i < 0) name = function;
      else name = function.substring(i + 1);
    }

    if (outputCls==null) {
      outputCls = fn.getOutputCls();
    } else if (outputCls.equalsIgnoreCase("none") ||
      outputCls.equalsIgnoreCase("void")) {
      outputCls = null;
    }
    if (fn.isImmutable())
      immutable = true;
  }

  public FunctionBinding replaceRelative(String pkgName) {
    if (function!=null && function.startsWith("."))
      function = pkgName + function;
    if (outputCls!=null && outputCls.startsWith("."))
      outputCls = pkgName + outputCls;
    return this;
  }
}
