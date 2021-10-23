package org.hpcclab.oaas.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;

@Data
@Accessors(chain = true)
public class DeepOaasFunctionBindingDto {
  OaasFunctionBinding.AccessModifier access;
  OaasFunctionDto function;
}
