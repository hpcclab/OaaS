package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Optional;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
@ProtoDoc("@Indexed")
public class OaasObject implements Copyable<OaasObject>, HasKey<String>, HasRev {

  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  String key;
  @ProtoField(1)
  String id;
  @ProtoField(value = 3, defaultValue = "-1")
  long revision = -1;
  @ProtoField(4)
  String cls;
  @ProtoField(6)
  OaasObjectState state;
  @ProtoField(7)
  Set<ObjectReference> refs;
  @ProtoField(value = 8)
  ObjectStatus status;
  @ProtoField(value = 10, javaType = ObjectNode.class)
  ObjectNode data;

  public OaasObject() {}

  @ProtoFactory
  public OaasObject(String id,
                    String cls,
                    OaasObjectState state, Set<ObjectReference> refs, ObjectNode data, ObjectStatus status,
                    long revision) {
    this.id = id;
    this.key = id;
    this.cls = cls;
    this.state = state;
    this.refs = refs;
    this.data = data;
    this.status = status;
    this.revision = revision;
  }

  public static OaasObject createFromClasses(OaasClass cls) {
    var o = new OaasObject();
    o.setCls(cls.getKey());
    o.setState(new OaasObjectState());
    return o;
  }

  public Optional<ObjectReference> findReference(String name) {
    return refs.stream()
      .filter(mem -> mem.getName().equals(name))
      .findFirst();
  }

  public OaasObject copy() {
    return new OaasObject(
      id,
      cls,
      state.copy(),
      refs==null ? null:Set.copyOf(refs),
      data != null? data.deepCopy(): null,
      status.copy(),
      revision
    );
  }




  @JsonIgnore
  public boolean isReadyToUsed() {
    return status.getTaskStatus().isCompleted() && !status.getTaskStatus().isFailed();
  }

  public OaasObject setId(String id) {
    this.id = id;
    setKey(id);
    return this;
  }
  public void setRevision(long revision) {
    this.revision = revision;
  }
}
