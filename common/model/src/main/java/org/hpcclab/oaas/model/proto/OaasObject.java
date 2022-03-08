package org.hpcclab.oaas.model.proto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hpcclab.oaas.model.object.ObjectReference;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectAccessModifier;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasObject {
  UUID id;
  OaasObjectOrigin origin;
  Long originHash;
  ObjectAccessModifier access = ObjectAccessModifier.PUBLIC;
  String cls;
  Set<String> labels;
  OaasObjectState state;
  Set<ObjectReference> refs;
  @JsonRawValue
  String embeddedRecord;
//  TaskCompletion task;


  public OaasObject() {
  }

  @ProtoFactory
  public OaasObject(UUID id, OaasObjectOrigin origin, Long originHash, ObjectAccessModifier access, String cls, Set<String> labels, OaasObjectState state, Set<ObjectReference> refs, String embeddedRecord) {
    this.id = id;
    this.origin = origin;
    this.originHash = originHash;
    this.access = access;
    this.cls = cls;
    this.labels = labels;
    this.state = state;
    this.refs = refs;
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
      access,
      cls,
      labels==null ? null:Set.copyOf(labels),
      state,
      refs==null ? null:Set.copyOf(refs),
      embeddedRecord
    );
  }

  @ProtoField(1)
  public UUID getId() {
    return id;
  }

  @ProtoField(2)
  public OaasObjectOrigin getOrigin() {
    return origin;
  }

  @ProtoField(3)
  public Long getOriginHash() {
    return originHash;
  }

  @ProtoField(4)
  public ObjectAccessModifier getAccess() {
    return access;
  }

  @ProtoField(5)
  @ProtoDoc("@Field(index=Index.YES, analyze = Analyze.NO, store = Store.YES)")
  public String getCls() {
    return cls;
  }

  @ProtoField(6)
  public Set<String> getLabels() {
    return labels;
  }

  @ProtoField(7)
  public OaasObjectState getState() {
    return state;
  }

  @ProtoField(8)
  public Set<ObjectReference> getRefs() {
    return refs;
  }

  @ProtoField(9)
  public String getEmbeddedRecord() {
    return embeddedRecord;
  }

  public void setEmbeddedRecord(JsonNode val) {
    this.embeddedRecord = val.toString();
  }

  public void setEmbeddedRecord(String embeddedRecord) {
    this.embeddedRecord = embeddedRecord;
  }
}
