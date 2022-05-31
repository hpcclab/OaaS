package org.hpcclab.oaas.controller.mapper;

import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CtxMapper {

  void set(OaasClass cls1, @MappingTarget OaasClass cls2);
}
