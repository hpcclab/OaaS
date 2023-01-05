package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.state.OaasObjectState;

import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
public class ObjectUpdate {
  ObjectNode data;
  Set<ObjectReference> refs;
  Set<String> updatedKeys;

  public ObjectUpdate() {
  }

  public ObjectUpdate(ObjectNode data) {
    this.data = data;
  }

  public ObjectUpdate(ObjectNode data,
                      Set<ObjectReference> refs,
                      Set<String> updatedKeys) {
    this.data = data;
    this.refs = refs;
    this.updatedKeys = updatedKeys;
  }
}
