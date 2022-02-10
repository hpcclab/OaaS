package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.model.DataAccessContext;
import org.hpcclab.oaas.model.proto.OaasClass;

public class DataAccessRequest {
  String oid;
  OaasClass cls;
  String key;
  DataAccessContext dac;

  public DataAccessRequest(String oid,
                           OaasClass cls,
                           String key,
                           DataAccessContext dac) {
    this.oid = oid;
    this.cls = cls;
    this.key = key;
    this.dac = dac;
  }

  public String getOid() {
    return oid;
  }

  public void setOid(String oid) {
    this.oid = oid;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public DataAccessContext getDac() {
    return dac;
  }

  public void setDac(DataAccessContext dac) {
    this.dac = dac;
  }

  public OaasClass getCls() {
    return cls;
  }

  public void setCls(OaasClass cls) {
    this.cls = cls;
  }
}
