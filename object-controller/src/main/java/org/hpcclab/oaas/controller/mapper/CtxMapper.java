package org.hpcclab.oaas.controller.mapper;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
  componentModel = MappingConstants.ComponentModel.JAKARTA,
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CtxMapper {

  void set(OaasClass cls1, @MappingTarget OaasClass cls2);
}
