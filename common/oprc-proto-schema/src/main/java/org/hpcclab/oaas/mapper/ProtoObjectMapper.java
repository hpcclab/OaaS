package org.hpcclab.oaas.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.io.IOException;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class ProtoObjectMapper {
  public abstract ProtoOObject toProto(OObject object);
  public abstract ProtoInvocationRequest toProto(InvocationRequest req);
  public abstract ProtoInvocationResponse toProto(InvocationResponse req);
  public  abstract OObject fromProto(ProtoOObject object);
  public  abstract InvocationRequest fromProto(ProtoInvocationRequest object);
  public abstract InvocationResponse fromProto(ProtoInvocationResponse resp);
  public abstract ProtoInvocationStatus convert(InvocationStatus status);
  public abstract InvocationStatus convert(ProtoInvocationStatus status);

  ObjectMapper mapper;

  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ByteString convert(ObjectNode objectNode) {
    try {
      return ByteString.copyFrom(mapper.writeValueAsBytes(objectNode));
    } catch (JsonProcessingException e) {
      throw new InvocationException("Json Error",e);
    }
  }
  public ObjectNode convert(ByteString bytes) {
    try {
      return mapper.readValue(bytes.toByteArray(), ObjectNode.class);
    } catch (IOException e) {
      throw new InvocationException("Json Error",e);
    }
  }
}
