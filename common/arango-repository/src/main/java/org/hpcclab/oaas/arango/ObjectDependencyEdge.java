package org.hpcclab.oaas.arango;

import com.arangodb.entity.From;
import com.arangodb.entity.Id;
import com.arangodb.entity.To;

public record ObjectDependencyEdge(
  @Id
  String id,
  @From
  String from,
  @To
  String to
) {
  public static ObjectDependencyEdge of(String from, String to) {
    return new ObjectDependencyEdge(null, from, to);
  }
}
