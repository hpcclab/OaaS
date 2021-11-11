package org.hpcclab.oaas.model.function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OaasFunctionBindingDto {
  FunctionAccessModifier access = FunctionAccessModifier.PUBLIC;
  @NotNull
  String function;

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || getClass()!=o.getClass()) return false;
    OaasFunctionBindingDto that = (OaasFunctionBindingDto) o;
    return access==that.access && Objects.equals(function, that.function);
  }

  @Override
  public int hashCode() {
    return Objects.hash(function);
  }
}
