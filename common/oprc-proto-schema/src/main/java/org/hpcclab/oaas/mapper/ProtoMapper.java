package org.hpcclab.oaas.mapper;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.cls.OClassDeploymentStatus;
import org.hpcclab.oaas.model.function.OFunctionDeploymentStatus;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.*;

import java.util.Map;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProtoMapper {
  ProtoOClass toProto(OClass cls);
  ProtoOFunction toProto(OFunction fn);
  ProtoOClassDeploymentStatus toProto(OClassDeploymentStatus status);
  ProtoOPackage toProto(OPackage pkg);
  ProtoOFunctionDeploymentStatus toProto(OFunctionDeploymentStatus status);


  OClass fromProto(ProtoOClass cls);
  OClassDeploymentStatus fromProto(ProtoOClassDeploymentStatus status);
  OFunction fromProto(ProtoOFunction fn);
  OFunctionDeploymentStatus fromProto(ProtoOFunctionDeploymentStatus status);
  OPackage fromProto(ProtoOPackage pkg);


  default DSMap map(Map<String,String> map) {
    if (map instanceof DSMap dsMap) return dsMap;
    return DSMap.copy(map);
  }

  default Map<String,String> map(DSMap map) {
    return map;
  }
}
