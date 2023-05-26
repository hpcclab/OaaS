package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class KvPair {
  @ProtoField(number = 1)
  String key;
  @ProtoField(number = 2)
  String val;

  public KvPair() {
  }

  public KvPair(Map.Entry<String, String> entry) {
    this.key = entry.getKey();
    this.val = entry.getValue();
  }


  @ProtoFactory
  public KvPair(String key, String val) {
    this.key = key;
    this.val = val;
  }

  public String getKey() {
    return key;
  }

  public String getVal() {
    return val;
  }

  public String setVal(String val) {
    var tmp = this.val;
    this.val = val;
    return tmp;
  }

  @Override
  public boolean equals(Object o) {
    if (this==o) return true;
    if (o==null || getClass()!=o.getClass()) return false;
    KvPair that = (KvPair) o;
    return Objects.equals(key, that.key) && Objects.equals(val, that.val);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, val);
  }

  public static Map<String, String> toMap(Collection<KvPair> pairs){
    if (pairs ==null || pairs.isEmpty())
      return Map.of();
    return pairs.stream().collect(Collectors.toMap(KvPair::getKey, KvPair::getVal));
  }
  public static Set<KvPair> fromMap(Map<String, String> map){
    if (map == null)
      return Set.of();
    return map.entrySet()
      .stream()
      .map(KvPair::new)
      .collect(Collectors.toSet());
  }
}
