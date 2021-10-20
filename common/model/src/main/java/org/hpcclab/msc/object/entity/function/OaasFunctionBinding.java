package org.hpcclab.msc.object.entity.function;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.util.Objects;

@Embeddable
@Data
@Accessors(chain = true)
public class OaasFunctionBinding {
  @NotNull
  @Column(nullable = false)
  AccessModifier access = AccessModifier.PUBLIC;
  @ManyToOne(fetch = javax.persistence.FetchType.LAZY)
  @NotNull
  OaasFunction function;

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    PRIVATE
  }

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || getClass()!=o.getClass()) return false;
    OaasFunctionBinding that = (OaasFunctionBinding) o;
    return access==that.access && function.equals(that.function);
  }

  @Override
  public int hashCode() {
    return Objects.hash(function.getName());
  }
}
