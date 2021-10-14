package org.hpcclab.msc.object.mapper;

import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.model.OaasFunctionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface FunctionMapper {
  @Mapping(target = "outputClasses", ignore = true)
  OaasFunction toFunc(OaasFunctionDto functionDto);
  OaasFunctionDto toFunc(OaasFunction function);
  List<OaasFunctionDto> toFunc(List<OaasFunction> function);
  @Mapping(target = "outputClasses", ignore = true)
  void set(OaasFunctionDto functionDto, @MappingTarget OaasFunction function);

  default String toName(OaasClass oaasClass) {
    return oaasClass.getName();
  }
}
