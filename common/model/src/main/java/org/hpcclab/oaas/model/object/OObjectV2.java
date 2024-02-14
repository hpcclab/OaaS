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
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;


@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OObjectV2 implements Copyable<OObjectV2>, HasKey<String>, HasRev {
  @ProtoField(1)
  Meta meta;
  @ProtoField(value = 2, javaType = ObjectNode.class)
  ObjectNode data;

  public OObjectV2() {
    meta = new Meta();
  }

  @ProtoFactory
  @JsonCreator
  public OObjectV2(Meta meta, ObjectNode data) {
    this.meta = meta;
    this.data = data;
  }

  public static OObjectV2 createFromClasses(OClass cls) {
    return new OObjectV2(new Meta(cls.getKey()), null);
  }

  public OObjectV2 copy() {
    return new OObjectV2(
      meta != null? meta.copy(): null,
      data
    );
  }

  @Override
  public long getRevision() {
    return meta!=null ? meta.getRevision():-1;
  }

  @Override
  public void setRevision(long revision) {
    if (meta!=null)
      meta.revision = revision;
  }

  @JsonProperty("_key")
  @JsonView(Views.Internal.class)
  public String getKey() {
    return meta != null? meta.id: null;
  }

  @Data
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  @Accessors(chain = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta implements Copyable<Meta> {
    @ProtoField(1)
    String id;
    @ProtoField(value = 2, defaultValue = "-1")
    long revision = -1;
    @ProtoField(3)
    String cls;
    @ProtoField(4)
    DSMap overrideUrls;
    @ProtoField(5)
    DSMap verIds;
    @ProtoField(6)
    DSMap refs;
    @ProtoField(value = 7, defaultValue = "-1")
    long lastOffset = -1;
    @ProtoField(8)
    String lastInv;

    @ProtoFactory
    public Meta(String id, long revision, String cls, DSMap overrideUrls, DSMap verIds, DSMap refs, long lastOffset, String lastInv) {
      this.id = id;
      this.revision = revision;
      this.cls = cls;
      this.overrideUrls = overrideUrls;
      this.verIds = verIds;
      this.refs = refs;
      this.lastOffset = lastOffset;
      this.lastInv = lastInv;
    }

    public Meta() {
    }

    public Meta(String cls) {
      this.cls = cls;
    }

    @Override
    public Meta copy() {
      return new Meta(
        id, revision, cls,
        DSMap.copy(overrideUrls),
        DSMap.copy(verIds),
        DSMap.copy(refs),
        lastOffset,
        lastInv
      );
    }
  }

}
