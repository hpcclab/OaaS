package org.hpcclab.oaas.entity.function;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.function.FunctionAccessModifier;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.util.Objects;

import static javax.persistence.CascadeType.*;

@Embeddable
@Data
@Accessors(chain = true)
public class OaasFunctionBinding {
  @NotNull
  @Column(nullable = false)
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;

  @JoinColumn
  @ManyToOne(fetch = javax.persistence.FetchType.LAZY, cascade = {DETACH, REFRESH})
  @NotNull
  OaasFunction function;


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
