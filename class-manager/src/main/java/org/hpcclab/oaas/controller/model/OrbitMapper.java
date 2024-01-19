package org.hpcclab.oaas.controller.model;

import org.hpcclab.oaas.proto.ProtoOrbit;
import org.hpcclab.oaas.proto.ProtoOrbitHash;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrbitMapper {
  OrbitHash map(ProtoOrbitHash orbitHashed);

  Orbit map(ProtoOrbit orbit);


  ProtoOrbitHash map(OrbitHash orbitHash);

  ProtoOrbit map(Orbit orbit);
}
