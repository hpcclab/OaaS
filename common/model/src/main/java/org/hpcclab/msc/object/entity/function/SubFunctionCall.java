package org.hpcclab.msc.object.entity.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubFunctionCall {
  String funcName;
  String target;
  List<String> inputRefs = List.of();
}
