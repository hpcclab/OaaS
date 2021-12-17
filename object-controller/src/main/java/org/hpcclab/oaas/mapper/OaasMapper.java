package org.hpcclab.oaas.mapper;

import org.hpcclab.oaas.entity.FunctionExecContext;
import org.hpcclab.oaas.model.cls.DeepOaasClassDto;
import org.hpcclab.oaas.model.object.DeepOaasObjectDto;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.model.proto.OaasObjectPb;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface OaasMapper {

  FunctionExecContext copy(FunctionExecContext ctx);
  void set(OaasClassPb cls1, @MappingTarget OaasClassPb cls2);
  @Mapping(target = "cls", ignore = true)
  DeepOaasObjectDto deep(OaasObjectPb object);
  @Mapping(target = "functions", ignore = true)
  DeepOaasClassDto deep(OaasClassPb cls);
}
