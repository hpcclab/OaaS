package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StringKvPair {
  @ProtoField(number = 1)
  String key;
  @ProtoField(number = 2)
  String value;

  public StringKvPair() {
  }

  public StringKvPair(Map.Entry<String, String> entry) {
    this.key = entry.getKey();
    this.value = entry.getValue();
  }


  @ProtoFactory
  public StringKvPair(String key, String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public String setValue(String value) {
    var tmp = this.value;
    this.value = value;
    return tmp;
  }

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || getClass()!=o.getClass()) return false;
    StringKvPair that = (StringKvPair) o;
    return Objects.equals(key, that.key) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
