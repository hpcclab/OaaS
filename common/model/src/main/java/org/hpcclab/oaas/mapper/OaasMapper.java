package org.hpcclab.oaas.mapper;

import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasCompoundMember;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface OaasMapper {
  @Mapping(target = "outputCls", ignore = true)
  OaasFunction toFunc(OaasFunctionDto functionDto);

  OaasFunctionDto toFunc(OaasFunction function);

  List<OaasFunctionDto> toFuncDto(List<OaasFunction> function);

  @Mapping(target = "outputCls", ignore = true)
  List<OaasFunction> toFunc(List<OaasFunctionDto> function);

  void set(OaasFunctionDto functionDto, @MappingTarget OaasFunction function);

  default String toName(OaasFunction function) {
    return function.getName();
  }

  default OaasFunction nameToFunc(String value) {
    return new OaasFunction().setName(value);
  }

  OaasClassDto toClass(OaasClass oaasClass);

  @Mapping(target = "functions", ignore = true)
  OaasClass toClass(OaasClassDto oaasClass);

  List<OaasClassDto> toClassDto(List<OaasClass> function);

  List<OaasClass> toClass(List<OaasClassDto> function);

  @Mapping(target = "functions", ignore = true)
  void set(OaasClassDto oaasClassDto, @MappingTarget OaasClass oaasClass);

  default String toName(OaasClass oaasClass) {
    if (oaasClass!=null)
      return oaasClass.getName();
    return null;
  }

  default OaasClass nameToClass(String value) {
    return new OaasClass().setName(value);
  }

  OaasObjectDto toObject(OaasObject object);

  @Mapping(target = "members", ignore = true)
  @Mapping(target = "cls", ignore = true)
  OaasObject toObject(OaasObjectDto object);

  List<OaasObjectDto> toObject(List<OaasObject> function);

  OaasCompoundMemberDto toMember(OaasCompoundMember member);

  OaasCompoundMember toMember(OaasCompoundMemberDto member);

  default UUID toUuid(OaasObject object) {
    return object.getId();
  }

  default OaasObject toObject(UUID value) {
    return new OaasObject().setId(value);
  }

  void set(OaasObjectDto objectDto, @MappingTarget OaasObject object);

  List<OaasFunctionBinding> toBinding(List<OaasFunctionBindingDto> bindingDto);

  default OaasFunctionBindingDto toBindingDto(OaasFunctionBinding binding) {
    return new OaasFunctionBindingDto().setAccess(binding.getAccess())
      .setFunction(binding.getFunction().getName());
  }

  List<OaasFunctionBindingDto> toBindingDto(List<OaasFunctionBinding> bindingDto);


  DeepOaasObjectDto deep(OaasObject object);

  DeepOaasClassDto deep(OaasClass cls);

  DeepOaasFunctionBindingDto deep(OaasFunctionBinding binding);

  FunctionExecContext copy(FunctionExecContext ctx);
}
