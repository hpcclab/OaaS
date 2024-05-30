package org.hpcclab.oaas.model.pkg;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OPackage {
  String name;
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  List<OClass> classes = List.of();
  @JsonSetter(nulls = Nulls.AS_EMPTY)
  List<OFunction> functions = List.of();
  List<String> required;
  boolean disable;
}
