package org.hpcclab.oaas.mapper;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.proto.*;
import org.mapstruct.*;

import java.util.Map;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ProtoMapper {
  ProtoOClass toProto(OClass cls);
  ProtoOFunction toProto(OFunction fn);
  ProtoOPackage toProto(OPackage pkg);
  OClass fromProto(ProtoOClass cls);
  OFunction fromProto(ProtoOFunction fn);
  OPackage fromProto(ProtoOPackage pkg);


  default DSMap map(Map<String,String> map) {
    if (map instanceof DSMap dsMap) return dsMap;
    return DSMap.copy(map);
  }

  default Map<String,String> map(DSMap map) {
    return map;
  }
}
