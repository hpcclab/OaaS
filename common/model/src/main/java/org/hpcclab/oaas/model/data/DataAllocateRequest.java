package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.state.KeySpecification;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAllocateRequest {
  String oid;
  String vid;
  List<KeySpecification> keys;
  String defaultProvider;
  boolean publicUrl = false;

  public DataAllocateRequest() {
  }

  public DataAllocateRequest(String oid, List<KeySpecification> keys, boolean publicUrl) {
    this.oid = oid;
    this.vid = oid;
    this.keys = keys;
    this.publicUrl = publicUrl;
  }

  public DataAllocateRequest(String oid, List<KeySpecification> keys, String defaultProvider, boolean publicUrl) {
    this.oid = oid;
    this.vid = oid;
    this.keys = keys;
    this.defaultProvider = defaultProvider;
    this.publicUrl = publicUrl;
  }

  public DataAllocateRequest(String oid,
                             String vid,
                             List<KeySpecification> keys,
                             String defaultProvider,
                             boolean publicUrl) {
    this.oid = oid;
    this.vid = vid;
    this.keys = keys;
    this.defaultProvider = defaultProvider;
    this.publicUrl = publicUrl;
  }
}
