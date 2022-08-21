package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Accessors(chain = true)
@ProtoDoc("@Indexed")
public class OaasObject implements Copyable<OaasObject> {

  @ProtoField(1)
  String id;
  @ProtoField(2)
  ObjectOrigin origin;
  @ProtoField(3)
  Long originHash;
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
  @JsonRawValue
  @ProtoField(10)
  String embeddedRecord;

  public OaasObject() {}

  @ProtoFactory
  public OaasObject(String id, ObjectOrigin origin, Long originHash, String cls, Set<String> labels, OaasObjectState state, Set<ObjectReference> refs, String embeddedRecord, ObjectStatus status, StreamInfo streamInfo) {
    this.id = id;
    this.origin = origin;
    this.originHash = originHash;
    this.cls = cls;
    this.labels = labels;
    this.state = state;
    this.refs = refs;
    this.embeddedRecord = embeddedRecord;
    this.status = status;
    this.streamInfo = streamInfo;
  }

  public static OaasObject createFromClasses(OaasClass cls) {
    var o = new OaasObject();
    o.setCls(cls.getName());
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
      originHash,
      cls,
      labels==null ? null:Set.copyOf(labels),
      state.copy(),
      refs==null ? null:Set.copyOf(refs),
      embeddedRecord,
      status.copy(),
      streamInfo == null? null:streamInfo.copy()
    );
  }

  @JsonSetter
  public OaasObject setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
    return this;
  }

  public OaasObject setEmbeddedRecord(String embeddedRecord) {
    this.embeddedRecord = embeddedRecord;
    return this;
  }

  public void updateStatus(TaskCompletion taskCompletion) {
    status.set(taskCompletion);
    if (taskCompletion.getEmbeddedRecord()!=null)
      setEmbeddedRecord(taskCompletion.getEmbeddedRecord());
  }

  @JsonIgnore
  public boolean isReadyToUsed() {
    return origin.isRoot() || (status.getTaskStatus().isCompleted() && !status.getTaskStatus().isFailed());
  }
}
