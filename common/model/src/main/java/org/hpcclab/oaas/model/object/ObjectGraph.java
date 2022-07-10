package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Sets;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ObjectGraph {
  @ProtoField(1)
  Set<String> nextIds;

  public ObjectGraph() {
  }

  @ProtoFactory
  public ObjectGraph(Set<String> nextIds) {
    this.nextIds = nextIds;
  }

  public Set<String> getNextIds() {
    if (nextIds == null) nextIds = Sets.mutable.empty();
    return nextIds;
  }
}
