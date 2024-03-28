package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.hpcclab.oaas.model.cls.OClass;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DataAccessRequest(
  String oid,
  String vid,
  OClass cls,
  String key,
  DataAccessContext dac
){
}
