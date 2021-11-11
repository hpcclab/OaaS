package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.object.OaasObjectDto;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext {
  OaasObjectDto output;
  OaasObjectDto parent;
  OaasFunctionDto function;
  List<OaasObjectDto> additionalInputs = List.of();
}
