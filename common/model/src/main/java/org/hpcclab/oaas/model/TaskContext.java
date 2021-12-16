package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.object.OaasObjectDto;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.hpcclab.oaas.model.proto.OaasObjectPb;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskContext {
  OaasObjectPb output;
  //  OaasClassDto outputClass;
  OaasObjectPb parent;
  OaasFunctionPb function;
  List<OaasObjectPb> additionalInputs = List.of();
}
