package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.OaasFunctionDto;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.service.FunctionService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class FunctionResource implements FunctionService {
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository classRepo;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
  @Inject
  OaasMapper oaasMapper;

  public Uni<List<OaasFunctionDto>> list() {
    return funcRepo.find(
        "select f from OaasFunction f left join fetch f.outputClasses")
      .list()
      .map(oaasMapper::toFuncDto);
  }

  @Transactional
  public Uni<OaasFunctionDto> create(boolean update, OaasFunctionDto functionDto) {
    return funcRepo.findById(functionDto.getName())
      .flatMap(fn -> {
        if (fn!=null && !update) {
          if (update) {
            oaasMapper.set(functionDto, fn);
            return funcRepo.persist(fn);
          } else {
            throw new NoStackException("Function with this name already exist.")
              .setCode(HttpResponseStatus.CONFLICT.code());
          }
        }
//        return classRepo.listByNames(functionDto.getOutputClasses())
//          .flatMap(classes -> {
//            var func = oaasMapper.toFunc(functionDto);
//            func.setOutputClasses(List.copyOf(classes));
//            return funcRepo.persist(func);
//          });

        return funcRepo.save(functionDto);
      })
      .call(funcRepo::flush)
      .map(oaasMapper::toFunc);
  }

  @Transactional
  public Multi<OaasFunctionDto> createByYaml(boolean update, String body) {
    try {
      var funcs = yamlMapper.readValue(body, OaasFunctionDto[].class);
      return Multi.createFrom().iterable(Arrays.asList(funcs))
        .onItem().transformToUniAndConcatenate(f -> create(update, f));
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<OaasFunctionDto> get(String funcName) {
    return funcRepo.findById(funcName)
      .onItem().ifNull().failWith(NotFoundException::new)
      .map(oaasMapper::toFunc);
  }
}
