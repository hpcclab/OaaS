package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObject;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RootMscObjectCreating {
  String sourceUrl;
  OaasObject.ObjectType type;
//  Map<String, MscFuncMetadata> functions = Map.of();
  List<String> functions = List.of();
}
