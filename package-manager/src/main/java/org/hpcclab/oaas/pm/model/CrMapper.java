package org.hpcclab.oaas.pm.model;

import org.hpcclab.oaas.proto.ProtoCr;
import org.hpcclab.oaas.proto.ProtoCrHash;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CrMapper {
  CrHash fromProto(ProtoCrHash crHashed);

  OClassRuntime fromProto(ProtoCr clsRuntime);
  ProtoCrHash toProto(CrHash crHash);

  ProtoCr toProto(OClassRuntime clsRuntime);
}
