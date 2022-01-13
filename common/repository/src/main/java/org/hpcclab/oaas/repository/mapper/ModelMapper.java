package org.hpcclab.oaas.repository.mapper;

import org.hpcclab.oaas.model.cls.DeepOaasClass;
import org.hpcclab.oaas.model.object.DeepOaasObject;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ModelMapper {

  void set(OaasClass cls1, @MappingTarget OaasClass cls2);
  @Mapping(target = "cls", ignore = true)
  DeepOaasObject deep(OaasObject object);
  @Mapping(target = "functions", ignore = true)
  DeepOaasClass deep(OaasClass cls);
  OaasClass copy(OaasClass cls);
}
