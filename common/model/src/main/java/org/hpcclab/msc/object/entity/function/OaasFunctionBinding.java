package org.hpcclab.msc.object.entity.function;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Embeddable
@Data
@Accessors(chain = true)
public class OaasFunctionBinding {
  AccessModifier access;
  @JoinColumn
  @ManyToOne(fetch = javax.persistence.FetchType.LAZY)
  OaasFunction function;

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    PRIVATE
  }
}
