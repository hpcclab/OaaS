package org.hpcclab.oaas.model.function;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;

@Data
@Accessors(chain = true)
public class DeepOaasFunctionBindingDto {
  FunctionAccessModifier access;
  OaasFunctionPb function;
}
