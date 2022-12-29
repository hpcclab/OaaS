package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ObjectUpdate {
  ObjectNode data;
  Set<ObjectReference> refs;

  public ObjectUpdate(ObjectNode data, Set<ObjectReference> refs) {
    this.data = data;
    this.refs = refs;
  }
}
