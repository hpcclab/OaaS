package org.hpcclab.oaas.model.object;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.HasKey;
import org.hpcclab.oaas.model.HasRev;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.IOException;


@Data
@Accessors(chain = true)
public class POObject implements IOObject<byte[]> {
  @ProtoField(1)
  OMeta meta;
  @ProtoField(2)
  byte[] data;

  public POObject() {
    meta = new OMeta();
  }

  @ProtoFactory
  public POObject(OMeta meta, byte[] data) {
    this.meta = meta;
    this.data = data;
  }

  public POObject copy() {
    return new POObject(
      meta.toBuilder().build(),
      data
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

  public String getKey() {
    return meta.id;
  }
}
