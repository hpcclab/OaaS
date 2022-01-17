package org.hpcclab.oaas.mapper;

import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface CtxMapper {

  FunctionExecContext copy(FunctionExecContext ctx);

  void set(OaasClass cls1, @MappingTarget OaasClass cls2);
}
