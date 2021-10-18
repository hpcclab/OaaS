package org.hpcclab.msc.object.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;

@Data
@Accessors(chain = true)
public class DeepOaasFunctionBindingDto {
  OaasFunctionBinding.AccessModifier access;
  OaasFunctionDto function;
}
