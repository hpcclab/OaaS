package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.model.DataAccessContext;

public class DataAccessRequest {
  String oid;
  String key;
  DataAccessContext dac;

  public DataAccessRequest(String oid,
                           String key,
                           DataAccessContext dac) {
    this.oid = oid;
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
}
