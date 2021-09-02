package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionExecContext {
  FunctionCallRequest request;
  MscObject target;
  MscObject compound;
  Map<String, MscObject> members = Map.of();
  Map<String, MscFunction> subFunctions = Map.of();
  MscFunction function;
  Map<String, String> args= Map.of();
  List<MscObject> additionalInputs = List.of();
}
