package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OaasClass;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataAccessRequest(
  String oid,
  String vid,
  OaasClass cls,
  String key,
  DataAccessContext dac
){
}
