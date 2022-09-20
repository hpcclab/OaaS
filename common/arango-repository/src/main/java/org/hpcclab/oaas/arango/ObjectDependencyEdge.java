package org.hpcclab.oaas.arango;

import com.arangodb.entity.From;
import com.arangodb.entity.Id;
import com.arangodb.entity.To;

public class ObjectDependencyEdge {
  @Id
  String id;
  @From
  String from;
  @To
  String to;

  public ObjectDependencyEdge() {
  }

  public ObjectDependencyEdge(String from, String to) {
    this.from = from;
    this.to = to;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }
}
