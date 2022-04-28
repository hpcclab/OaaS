package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.object.StreamInfo;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@ProtoDoc("@Indexed")
public class OaasObject {

  @ProtoField(1)
  String id;
  @ProtoField(2)
  OaasObjectOrigin origin;
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
  TaskCompletion task;
  @ProtoField(11)
  StreamInfo streamInfo;

  public OaasObject() {
  }

  @ProtoFactory
  public OaasObject(String id, OaasObjectOrigin origin, Long originHash, String cls, Set<String> labels, OaasObjectState state, Set<ObjectReference> refs, String embeddedRecord, TaskCompletion task, StreamInfo streamInfo) {
    this.id = id;
    this.origin = origin;
    this.originHash = originHash;
    this.cls = cls;
    this.labels = labels;
    this.state = state;
    this.refs = refs;
    this.embeddedRecord = embeddedRecord;
    this.task = task;
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
}
