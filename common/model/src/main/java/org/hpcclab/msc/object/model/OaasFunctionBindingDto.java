package org.hpcclab.msc.object.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;

import javax.persistence.Embeddable;

@Data
@Accessors(chain = true)
public class OaasFunctionBindingDto {
  OaasFunctionBinding.AccessModifier access;
  String function;
}
