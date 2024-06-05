package org.hpcclab.oaas.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassDeploymentStatus;
import org.hpcclab.oaas.model.cr.CrHash;
import org.hpcclab.oaas.model.cr.OClassRuntime;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.function.OFunctionConfig;
import org.hpcclab.oaas.model.function.OFunctionDeploymentStatus;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.model.object.POObject;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.io.IOException;
import java.util.Map;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProtoMapper {
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
  POObject fromProto(ProtoPOObject poObject);
  ProtoPOObject toProto(POObject poObject);

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
}
