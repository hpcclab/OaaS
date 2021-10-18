package org.hpcclab.msc.object.mapper;

import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface OaasMapper {
  OaasFunction toFunc(OaasFunctionDto functionDto);

  OaasFunctionDto toFunc(OaasFunction function);

  List<OaasFunctionDto> toFunc(List<OaasFunction> function);

  @Mapping(target = "outputClasses", ignore = true)
  void set(OaasFunctionDto functionDto, @MappingTarget OaasFunction function);

  default String toName(OaasFunction function) {
    return function.getName();
  }

  default OaasFunction nameToFunc(String value) {
    return new OaasFunction().setName(value);
  }

  OaasClassDto toClass(OaasClass oaasClass);

  OaasClass toClass(OaasClassDto oaasClass);

  List<OaasClassDto> toClass(List<OaasClass> function);

  void set(OaasClassDto oaasClassDto, @MappingTarget OaasClass oaasClass);

  default String toName(OaasClass oaasClass) {
    return oaasClass.getName();
  }

  default OaasClass nameToClass(String value) {
    return new OaasClass().setName(value);
  }

  OaasObjectDto toObject(OaasObject object);

  OaasObject toObject(OaasObjectDto object);

  List<OaasObjectDto> toObject(List<OaasObject> function);

  void set(OaasObjectDto objectDto, @MappingTarget OaasObject object);

  default OaasFunctionBinding toBinding(OaasFunctionBindingDto bindingDto) {
    return new OaasFunctionBinding().setAccess(bindingDto.getAccess())
      .setFunction(new OaasFunction().setName(bindingDto.getFunction()));
  }

  List<OaasFunctionBinding> toBinding(List<OaasFunctionBindingDto> bindingDto);

  default OaasFunctionBindingDto toBindingDto(OaasFunctionBinding binding) {
    return new OaasFunctionBindingDto().setAccess(binding.getAccess())
      .setFunction(binding.getFunction().getName());
  }

  List<OaasFunctionBindingDto> toBindingDto(List<OaasFunctionBinding> bindingDto);


  DeepOaasObjectDto deep(OaasObject object);
  DeepOaasClassDto deep(OaasClass cls);
  DeepOaasFunctionBindingDto deep(OaasFunctionBinding binding);
}
