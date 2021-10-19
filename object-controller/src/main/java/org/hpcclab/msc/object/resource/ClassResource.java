package org.hpcclab.msc.object.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasClassDto;
import org.hpcclab.msc.object.model.OaasFunctionDto;
import org.hpcclab.msc.object.repository.OaasClassRepository;
import org.hpcclab.msc.object.service.ClassService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class ClassResource implements ClassService {
  @Inject
  OaasClassRepository classRepo;
  @Inject
  OaasMapper oaasMapper;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());


  @Override
  public Uni<List<OaasClassDto>> list() {
    return classRepo.find(
      "select c from OaasClass c left join fetch c.functions")
      .list()
      .map(oaasMapper::toClassDto);
  }

  @Override
  @ReactiveTransactional
  public Uni<OaasClassDto> create(boolean update, OaasClassDto classDto) {
    return classRepo.persist(oaasMapper.toClass(classDto))
      .map(oaasMapper::toClass);
  }

  @Override
  @ReactiveTransactional
  public Uni<OaasClassDto> patch(String name, OaasClassDto classDto) {
    return classRepo.findById(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(cls -> {
        oaasMapper.set(classDto, cls);
        return classRepo.persist(cls);
      })
      .map(oaasMapper::toClass);
  }

  @Override
  public Uni<OaasClassDto> createByYaml(boolean update, String body) {
    try {
      var func = yamlMapper.readValue(body, OaasClassDto.class);
      return create(update, func);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  @Override
  public Uni<OaasClassDto> get(String name) {
    return classRepo.findById(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .map(oaasMapper::toClass);
  }

  @Override
  public Uni<OaasClassDto> delete(String name) {
    return classRepo.findById(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .call(cls -> classRepo.delete(cls))
      .map(oaasMapper::toClass);
  }
}
