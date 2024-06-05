package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.Views;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;


@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JOObject implements IOObject<ObjectNode> {
  OMeta meta;
  ObjectNode data;

  public JOObject() {
    meta = new OMeta();
  }

  public JOObject(OMeta meta, ObjectNode data) {
    this.meta = meta;
    this.data = data;
  }

  public JOObject copy() {
    return new JOObject(
      meta.toBuilder().build(),
      data.deepCopy()
    );
  }

  @Override
  public long getRevision() {
    return meta.getRevision();
  }

  @Override
  public void setRevision(long revision) {
    meta = meta.toBuilder()
      .revision(revision)
      .build();
  }

  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  public String getKey() {
    return meta.id;
  }
}
