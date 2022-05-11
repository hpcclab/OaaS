package org.hpcclab.oaas.model.function;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DeepOaasFunctionBinding {
  FunctionAccessModifier access;
  OaasFunction function;
  String alias;
}
