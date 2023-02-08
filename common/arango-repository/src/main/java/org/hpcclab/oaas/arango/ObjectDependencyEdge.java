package org.hpcclab.oaas.arango;

import com.arangodb.entity.From;
import com.arangodb.entity.Id;
import com.arangodb.entity.To;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;

public final class ObjectDependencyEdge {
  @Id
  String id;
  @From
  String from;
  @To
  String to;

  public ObjectDependencyEdge() {
  }

  public ObjectDependencyEdge(
    String id,
    String from,
    String to) {
    this.id = id;
    this.from = from;
    this.to = to;
  }

  public static ObjectDependencyEdge of(String from, String to) {
    return new ObjectDependencyEdge(null, from, to);
  }

  public String getId() {
    return id;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj==this) return true;
    if (obj==null || obj.getClass()!=this.getClass()) return false;
    var that = (ObjectDependencyEdge) obj;
    return Objects.equals(this.id, that.id) &&
      Objects.equals(this.from, that.from) &&
      Objects.equals(this.to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, from, to);
  }

  @Override
  public String toString() {
    return "ObjectDependencyEdge[" +
      "id=" + id + ", " +
      "from=" + from + ", " +
      "to=" + to + ']';
  }

}
