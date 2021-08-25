package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.MscFuncMetadata;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RootMscObjectCreating {
  String sourceUrl;
  String type;
  Map<String, MscFuncMetadata> functions = Map.of();
}
