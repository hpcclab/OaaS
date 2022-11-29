package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasPackage {
  String name;
  List<String> required;

  public static String extractPackageName(String name) {
    var last = name.lastIndexOf('.');
    if (last > 0) {
      return name.substring(0, last);
    }
    return "";
  }
}
