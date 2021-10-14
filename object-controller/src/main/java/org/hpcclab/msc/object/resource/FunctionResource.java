package org.hpcclab.msc.object.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.mapper.FunctionMapper;
import org.hpcclab.msc.object.model.OaasFunctionDto;
import org.hpcclab.msc.object.repository.OaasClassRepository;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.hpcclab.msc.object.service.FunctionService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class FunctionResource implements FunctionService {
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository classRepo;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  @Inject
  FunctionMapper functionMapper;

  public Uni<List<OaasFunctionDto>> list() {
    return funcRepo.listAll()
      .map(functionMapper::toFunc);
  }

  @Transactional
  public Uni<OaasFunctionDto> create(boolean update, OaasFunctionDto functionDto) {
    return funcRepo.findByName(functionDto.getName())
      .flatMap(fn -> {
        if (fn != null && !update) {
          if (update) {
            functionMapper.set(functionDto, fn);
            return funcRepo.persist(fn);
          } else {
            throw new NoStackException("Function with this name already exist.")
              .setCode(HttpResponseStatus.CONFLICT.code());
          }
        }
        return classRepo.listByNames(functionDto.getOutputClasses())
          .flatMap(classes -> {
            var func = functionMapper.toFunc(functionDto);
            func.setOutputClasses(Set.copyOf(classes));
            return funcRepo.persist(func);
          });
      })
      .call(funcRepo::flush)
      .map(functionMapper::toFunc);
  }

  @Override
  public Uni<OaasFunctionDto> createByYaml(boolean update, String body) {
    try {
      var func = yamlMapper.readValue(body, OaasFunctionDto.class);
      return create(update, func);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<OaasFunctionDto> get(String funcName) {
    return funcRepo.findByName(funcName)
      .invoke(f -> {
        if (f==null)
          throw new NotFoundException();
      })
      .map(functionMapper::toFunc);
  }
}
