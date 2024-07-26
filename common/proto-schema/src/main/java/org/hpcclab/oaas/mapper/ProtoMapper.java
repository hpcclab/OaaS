package org.hpcclab.oaas.mapper;

import com.google.protobuf.ByteString;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassDeploymentStatus;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.cr.OClassRuntime;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;
import org.hpcclab.oaas.model.function.OFunctionDeploymentStatus;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStatus;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.object.JsonObjectBytes;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.OTaskCompletion;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

import java.util.Map;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProtoMapper {
  ProtoMapper INSTANCE = Mappers.getMapper( ProtoMapper.class );

  ProtoOClass toProto(OClass cls);

  ProtoOFunction toProto(OFunction fn);

  ProtoOFunctionConfig toProto(OFunctionConfig fn);

  ProtoOClassDeploymentStatus toProto(OClassDeploymentStatus status);

  ProtoOPackage toProto(OPackage pkg);

  ProtoOFunctionDeploymentStatus toProto(OFunctionDeploymentStatus status);

  OClass fromProto(ProtoOClass cls);

  OClassDeploymentStatus fromProto(ProtoOClassDeploymentStatus status);

  OFunction fromProto(ProtoOFunction fn);

  OFunctionConfig fromProto(ProtoOFunctionConfig fn);

  OFunctionDeploymentStatus fromProto(ProtoOFunctionDeploymentStatus status);

  ProvisionConfig fromProto(ProtoProvisionConfig config);

  ProtoProvisionConfig toProto(ProvisionConfig config);

  OPackage fromProto(ProtoOPackage pkg);

  CrHash fromProto(ProtoCrHash crHashed);

  OClassRuntime fromProto(ProtoCr clsRuntime);

  ProtoCrHash toProto(CrHash crHash);

  ProtoCr toProto(OClassRuntime clsRuntime);
  OMeta fromProto(ProtoOMeta oMeta);
  ProtoOMeta toProto(OMeta oMeta);
  GOObject fromProto(ProtoPOObject obj);
  ProtoPOObject toProto(GOObject obj);

  ProtoInvocationRequest toProto(InvocationRequest req);

  ProtoInvocationResponse toProto(InvocationResponse req);

  InvocationRequest fromProto(ProtoInvocationRequest object);

  InvocationResponse fromProto(ProtoInvocationResponse resp);

  ProtoOTask toProto(OTask task);
  ProtoOTaskCompletion toProto(OTaskCompletion taskCompletion);
  OTask fromProto(ProtoOTask task);
  OTaskCompletion fromProto(ProtoOTaskCompletion taskCompletion);


  ProtoInvocationStatus convert(InvocationStatus status);

  InvocationStatus convert(ProtoInvocationStatus status);


  default DSMap map(Map<String, String> map) {
    if (map instanceof DSMap dsMap) return dsMap;
    return DSMap.copy(map);
  }

  default Map<String, String> map(DSMap map) {
    return map;
  }

  default byte[] convert(ByteString bytes) {
    if (bytes == null) return new byte[0];
    return bytes.toByteArray();
  }
  default ByteString convert(byte[] bytes) {
    if (bytes == null) return ByteString.EMPTY;
    return ByteString.copyFrom(bytes);
  }
  default JsonBytes toJsonBytes(ByteString bytes) {
    if (bytes == null) return JsonBytes.EMPTY;
    return new JsonBytes(bytes.toByteArray());
  }
  default ByteString fromJsonBytes(JsonBytes jsonBytes) {
    if (jsonBytes == null) return ByteString.EMPTY;
    return ByteString.copyFrom(jsonBytes.getBytes());
  }

  default JsonObjectBytes toJsonObjectBytes(ByteString bytes) {
    if (bytes == null) return JsonObjectBytes.EMPTY;
    return new JsonObjectBytes(bytes.toByteArray());
  }

  default ByteString fromJsonObjectBytes(JsonObjectBytes jsonBytes) {
    if (jsonBytes == null) return ByteString.EMPTY;
    return ByteString.copyFrom(jsonBytes.getBytes());
  }

  default Map<String, Object> toJsonMap(ByteString bytes) {
    if (bytes == null) return Map.of();
    if (bytes.isEmpty()) return Map.of();
    return new JsonObject(bytes.toStringUtf8()).getMap();
  }
  default ByteString fromJsonMap(Map<String, Object> map) {
    if (map == null) return ByteString.EMPTY;
    return ByteString.copyFromUtf8(new JsonObject(map).toString());
  }
}
