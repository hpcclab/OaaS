package org.hpcclab.oaas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OaasFunctionBindingDto {
  OaasFunctionBinding.AccessModifier access = OaasFunctionBinding.AccessModifier.PUBLIC;
  String function;
}
