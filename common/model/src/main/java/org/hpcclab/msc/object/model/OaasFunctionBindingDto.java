package org.hpcclab.msc.object.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;

import javax.persistence.Embeddable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OaasFunctionBindingDto {
  OaasFunctionBinding.AccessModifier access = OaasFunctionBinding.AccessModifier.PUBLIC;
  String function;
}
