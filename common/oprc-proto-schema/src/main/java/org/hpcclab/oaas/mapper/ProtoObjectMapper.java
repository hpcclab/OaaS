package org.hpcclab.oaas.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import org.hpcclab.oaas.model.cr.OClassRuntime;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.IOObject;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ProtoObjectMapper {
  private static final Logger logger = LoggerFactory.getLogger(ProtoObjectMapper.class);
  ObjectMapper mapper;

  public abstract ProtoOObject toProto(OObject object);

  public abstract ProtoInvocationRequest toProto(InvocationRequest req);

  public abstract ProtoInvocationResponse toProto(InvocationResponse req);

  public abstract OObject fromProto(ProtoOObject object);

  public abstract InvocationRequest fromProto(ProtoInvocationRequest object);

  public abstract InvocationResponse fromProto(ProtoInvocationResponse resp);

  public abstract ProtoInvocationStatus convert(InvocationStatus status);

  public abstract InvocationStatus convert(ProtoInvocationStatus status);
  public abstract OMeta fromProto(ProtoOMeta oMeta);
  public abstract  ProtoOMeta toProto(OMeta oMeta);
  public IOObject fromProto(ProtoPOObject poObject){
    OMeta meta = fromProto(poObject.getMeta());
    return new POObject(meta, poObject.toByteArray());
  }
  public ProtoPOObject toProto(IOObject ioObject){
    ProtoOMeta meta = toProto((OMeta) ioObject.getMeta());
    return ProtoPOObject.newBuilder()
      .setMeta(meta)
      .setData(ByteString.copyFrom((byte[]) ioObject.getData()))
      .build();
  }

  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ByteString convert(ObjectNode objectNode) {
    try {
      if (objectNode==null) return ByteString.EMPTY;
      var b = mapper.writeValueAsBytes(objectNode);
      return ByteString.copyFrom(b);
    } catch (JsonProcessingException e) {
      throw new InvocationException("Json writing error:" + e.getMessage(), e);
    }
  }

  public ObjectNode convert(ByteString bytes) {
    var b = bytes.toByteArray();
    try {
      if (b.length==0) return null;
      return mapper.readValue(b, ObjectNode.class);
    } catch (IOException e) {
      if (logger.isDebugEnabled())
        logger.debug("parse error '{}'", bytes.toStringUtf8());
      throw new InvocationException("Json parsing error:" + e.getMessage(), e);
    }
  }

  public byte[] toBytes(ByteString bytes) {
    if (bytes == null) return new byte[0];
    return bytes.toByteArray();
  }
  public ByteString fromBytes(byte[] bytes) {
    if (bytes == null) return ByteString.EMPTY;
    return ByteString.copyFrom(bytes);
  }
}
