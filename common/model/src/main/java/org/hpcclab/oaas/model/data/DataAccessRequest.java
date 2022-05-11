package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OaasClass;
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAccessRequest {
  String oid;
  OaasClass cls;
  String key;
  DataAccessContext dac;

  public DataAccessRequest(String oid, OaasClass cls, String key, DataAccessContext dac) {
    this.oid = oid;
    this.cls = cls;
    this.key = key;
    this.dac = dac;
  }
}
