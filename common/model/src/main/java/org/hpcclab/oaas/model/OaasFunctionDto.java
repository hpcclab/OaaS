package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunctionValidation;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasWorkflow;
import org.hpcclab.oaas.entity.task.TaskConfiguration;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasFunctionDto {
  @NotBlank
  String name;

  @NotNull
  OaasFunction.FuncType type;

  String outputCls;

  OaasFunctionValidation validation;

  TaskConfiguration task;

  OaasWorkflow macro;
}
