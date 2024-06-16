package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;


@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GOObject implements IOObject<JsonBytes> {
  @ProtoField(1)
  @JsonProperty("_meta")
  OMeta meta;
  @ProtoField(2)
  JsonBytes data;

  public GOObject() {
    meta = new OMeta();
    data = JsonBytes.EMPTY;
  }

  public GOObject(OMeta meta) {
    this.meta = meta;
    this.data = JsonBytes.EMPTY;
  }

  @ProtoFactory
  public GOObject(OMeta meta, JsonBytes data) {
    this.meta = meta;
    this.data = data;
  }

  public GOObject(OMeta meta, ObjectNode data) {
    this.meta = meta;
    this.data = new JsonBytes(data);
  }

  public GOObject(OMeta meta, byte[] data) {
    this.meta = meta;
    this.data = new JsonBytes(data);
  }

  public GOObject copy() {
    return new GOObject(
      meta.copy(),
      data
    );
  }

  @Override
  @JsonIgnore
  public long getRevision() {
    return meta.getRevision();
  }

  @Override
  @JsonIgnore
  public void setRevision(long revision) {
    meta.revision = revision;
  }

  @JsonIgnore
  public String getKey() {
    return meta.id;
  }
}
