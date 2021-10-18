package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunctionValidation;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasWorkflow;
import org.hpcclab.msc.object.entity.task.TaskConfiguration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionDto {
  @NotBlank
  String name;

  @NotNull
  OaasFunction.FuncType type;

  List<String> outputClasses;

  OaasFunctionValidation validation;

  TaskConfiguration task;

  OaasWorkflow macro;
}
