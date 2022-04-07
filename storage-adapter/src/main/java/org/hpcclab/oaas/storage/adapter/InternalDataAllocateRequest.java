package org.hpcclab.oaas.storage.adapter;


import java.util.List;

public class InternalDataAllocateRequest {
  String id;
  List<String> keys;
  String provider;
  boolean publicUrl = false;

  public InternalDataAllocateRequest() {
  }

  public InternalDataAllocateRequest(String id, List<String> keys, String provider, boolean publicUrl) {
    this.id = id;
    this.keys = keys;
    this.provider = provider;
    this.publicUrl = publicUrl;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<String> getKeys() {
    return keys;
  }

  public void setKeys(List<String> keys) {
    this.keys = keys;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public boolean isPublicUrl() {
    return publicUrl;
  }

  public void setPublicUrl(boolean publicUrl) {
    this.publicUrl = publicUrl;
  }
}
