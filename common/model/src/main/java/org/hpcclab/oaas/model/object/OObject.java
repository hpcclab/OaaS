package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;


@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Accessors(chain = true)
public class OObject implements Copyable<OObject>, HasKey<String>, HasRev {
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
  DSMap refs;
  @ProtoField(value = 8, javaType = ObjectNode.class)
  ObjectNode data;
  @ProtoField(value = 9, defaultValue = "-1")
  long lastOffset = -1;
  @ProtoField(10)
  String lastInv;

  public OObject() {}

  @ProtoFactory
  public OObject(String id, long revision, String cls, OaasObjectState state, DSMap refs, ObjectNode data, long lastOffset, String lastInv) {
    this.id = id;
    this.key = id;
    this.revision = revision;
    this.cls = cls;
    this.state = state;
    this.refs = refs;
    this.data = data;
    this.lastOffset = lastOffset;
    this.lastInv = lastInv;
  }

  public static OObject createFromClasses(OClass cls) {
    var o = new OObject();
    o.setCls(cls.getKey());
    o.setState(new OaasObjectState());
    return o;
  }

  public OObject copy() {
    return new OObject(
      id,
      revision,
      cls,
      state.copy(),
      refs==null ? null:DSMap.copy(refs),
      data != null? data.deepCopy(): null,
      lastOffset,
      lastInv
    );
  }

  public OObject setId(String id) {
    this.id = id;
    setKey(id);
    return this;
  }
  public void setRevision(long revision) {
    this.revision = revision;
  }
}
