package org.hpcclab.oaas.model.pkg;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OPackage {
  String name;
  List<OClass> classes = List.of();
  List<OFunction> functions = List.of();
  List<String> required;
  Map<String, String> options = Map.of();
}
