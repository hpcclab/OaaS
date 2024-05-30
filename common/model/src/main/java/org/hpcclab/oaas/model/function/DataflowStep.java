package org.hpcclab.oaas.model.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.DSMap;

import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataflowStep (
  String function,
  String target,
  String targetCls,
  String as,
  List<DataMapperDefinition> inputDataMaps,
  DSMap args,
  DSMap argRefs
) {
}
