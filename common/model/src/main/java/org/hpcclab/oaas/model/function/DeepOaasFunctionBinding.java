package org.hpcclab.oaas.model.function;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasFunction;

@Data
@Accessors(chain = true)
public class DeepOaasFunctionBinding {
  FunctionAccessModifier access;
  OaasFunction function;
  String alias;
}
