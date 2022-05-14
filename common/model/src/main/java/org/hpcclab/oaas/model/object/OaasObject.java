package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.eclipse.collections.api.factory.Lists;
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
@ProtoDoc("@Indexed")
public class OaasObject {

  @ProtoField(1)
  String id;
  @ProtoField(2)
  ObjectOrigin origin;
  @ProtoField(3)
  Long originHash;
//  @ProtoField(4)
//  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;
  @ProtoField(5)
  @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
  String cls;
  @ProtoField(6)
  Set<String> labels;
  @ProtoField(7)
  OaasObjectState state;
  @ProtoField(8)
  Set<ObjectReference> refs;
  @JsonRawValue
  @ProtoField(9)
  String embeddedRecord;
  @ProtoField(10)
  ObjectStatus status;
  @ProtoField(11)
  StreamInfo streamInfo;

  public OaasObject() {
  }

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
    o.setState(new OaasObjectState()
      .setType(cls.getStateType()));
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
      state,
      refs==null ? null:Set.copyOf(refs),
      embeddedRecord,
      null,
      streamInfo
    );
  }

  @JsonSetter
  public void setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
  }

  public void setEmbeddedRecord(String embeddedRecord) {
    this.embeddedRecord = embeddedRecord;
  }

  public void updateStatus(TaskCompletion taskCompletion) {
    if (status == null) status = new ObjectStatus();
    status.set(taskCompletion);
  }

  @JsonIgnore
  public boolean isReadyToUsed() {
    return origin.isRoot() || status.getTaskStatus() == TaskStatus.SUCCEEDED;
  }

  public List<String> waitForList(){
    if (origin.isRoot()) {
      return List.of();
    }
    var list = refs != null && !refs.isEmpty() ?
      Lists.fixedSize.ofAll(refs).collect(ObjectReference::getObjId):
      Lists.mutable.<String>empty();
    if (origin.isWfi() && origin.getInputs()!= null) {
      list.addAll(origin.getInputs());
    }
    if (refs != null) {
       list.addAll(Lists.fixedSize.ofAll(refs)
         .collect(ObjectReference::getObjId));
    }
    list.add(origin.getParentId());
    return list;
  }
}
