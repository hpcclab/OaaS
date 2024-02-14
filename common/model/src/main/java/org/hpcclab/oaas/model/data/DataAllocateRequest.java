package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAllocateRequest {
  String oid;
  String vid;
  List<String> keys;
  String provider;
  boolean publicUrl = false;

  public DataAllocateRequest() {
  }


  public DataAllocateRequest(String oid, List<String> keys, String provider, boolean publicUrl) {
    this.oid = oid;
    this.vid = oid;
    this.keys = keys;
    this.provider = provider;
    this.publicUrl = publicUrl;
  }

  public DataAllocateRequest(String oid,
                             String vid,
                             List<String> keys,
                             String provider,
                             boolean publicUrl) {
    this.oid = oid;
    this.vid = vid;
    this.keys = keys;
    this.provider = provider;
    this.publicUrl = publicUrl;
  }
}
