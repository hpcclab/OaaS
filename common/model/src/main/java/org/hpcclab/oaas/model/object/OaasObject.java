package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
@ProtoDoc("@Indexed")
public class OaasObject implements Copyable<OaasObject> {

  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  String key;

  @JsonProperty("_rev")
  @JsonView(Views.Internal.class)
  String rev;
  @ProtoField(1)
  String id;
  @ProtoField(2)
  ObjectOrigin origin;
  @ProtoField(3)
  Long hash;
  @ProtoField(4)
  @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
  String cls;
  @ProtoField(5)
  Set<String> labels;
  @ProtoField(6)
  OaasObjectState state;
  @ProtoField(7)
  Set<ObjectReference> refs;
  @ProtoField(value = 8)
  ObjectStatus status;
  @ProtoField(9)
  StreamInfo streamInfo;
  @ProtoField(value = 10, javaType = ObjectNode.class)
  ObjectNode data;

  public OaasObject() {}

  @ProtoFactory
  public OaasObject(String id, ObjectOrigin origin, Long hash, String cls, Set<String> labels, OaasObjectState state, Set<ObjectReference> refs, ObjectNode data, ObjectStatus status, StreamInfo streamInfo) {
    this.id = id;
    this.key = id;
    this.origin = origin;
    this.hash = hash;
    this.cls = cls;
    this.labels = labels;
    this.state = state;
    this.refs = refs;
    this.data = data;
    this.status = status;
    this.streamInfo = streamInfo;
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
      origin==null ? null:origin.copy(),
      hash,
      cls,
      labels==null ? null:Set.copyOf(labels),
      state.copy(),
      refs==null ? null:Set.copyOf(refs),
      data,
      status.copy(),
      streamInfo == null? null:streamInfo.copy()
    );
  }

  public void updateStatus(TaskCompletion taskCompletion) {
    status.set(taskCompletion);
    if (taskCompletion.getOutput() != null)
      taskCompletion.getOutput().update(this, taskCompletion.getVId());
  }



  @JsonIgnore
  public boolean isReadyToUsed() {
    return origin.isRoot() || (status.getTaskStatus().isCompleted() && !status.getTaskStatus().isFailed());
  }

  public OaasObject setId(String id) {
    this.id = id;
    setKey(id);
    return this;
  }

  public OaasObject markAsSubmitted(String originator,
                                    boolean queue) {
    var ts = status.getTaskStatus();
    if (ts.isSubmitted() || ts.isFailed())
      return this;
    if (originator == null) originator = id;
    status
      .setTaskStatus(TaskStatus.DOING)
      .setOriginator(originator);
    if (queue)
      status.setQueTs(System.currentTimeMillis());
    else
      status.setSmtTs(System.currentTimeMillis());

    return this;
  }

  public OaasObject markAsFailed() {
    var ts = status.getTaskStatus();
    if (ts.isSubmitted() || ts.isFailed())
      return this;
    status
      .setTaskStatus(TaskStatus.DEPENDENCY_FAILED);
    return this;
  }
}
