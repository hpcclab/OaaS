package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.types.ObjectId;
import org.hpcclab.msc.object.entity.object.MscObjectOrigin;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionCallRequest {
  ObjectId target;
  @NotBlank
  String functionName;
  Map<String, String> args;
  List<ObjectId> additionalInputs;

  public static FunctionCallRequest from(MscObjectOrigin origin) {
    return new FunctionCallRequest()
      .setTarget(origin.getParentId())
      .setFunctionName(origin.getFuncName())
      .setArgs(origin.getArgs())
      .setAdditionalInputs(origin.getAdditionalInputRefs());
  }
}
