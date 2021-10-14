package org.hpcclab.msc.object.entity.function;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Embeddable
@Data
@Accessors(chain = true)
public class OaasFunctionBinding {
  AccessModifier access;
  @ManyToOne
  OaasFunction function;

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    PRIVATE
  }
}
