package org.hpcclab.oaas.invoker.ispn.edge;


import com.arangodb.entity.Key;
import org.hpcclab.oaas.model.HasKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ObjInvNode implements HasKey, Serializable {
  @Key
  String key;
  List<String> nextInv;

  public ObjInvNode() {
  }

  public ObjInvNode(String key) {
    this.key = key;
    nextInv = new ArrayList<>();
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getNextInv() {
    return nextInv;
  }

  public void setNextInv(List<String> nextInv) {
    this.nextInv = nextInv;
  }

  @Override
  public String toString() {
    return "ObjInvNode{" +
      "key='" + key + '\'' +
      ", nextInv=" + nextInv +
      '}';
  }
}
