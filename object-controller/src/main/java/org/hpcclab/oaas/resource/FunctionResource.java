package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.exception.NoStackException;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.OaasFunctionDto;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.service.FunctionService;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import java.util.Arrays;
import java.util.List;

@RequestScoped
public class FunctionResource implements FunctionService {
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository classRepo;
  @Inject
  OaasMapper oaasMapper;
  @Inject
  Mutiny.Session session;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public Uni<List<OaasFunctionDto>> list() {
    return funcRepo.findAll()
      .list()
      .map(oaasMapper::toFuncDto);
  }

  @ReactiveTransactional
  public Uni<List<OaasFunctionDto>> create(boolean update, List<OaasFunctionDto> functionDtos) {
    return Multi.createFrom().iterable(functionDtos)
      .call(funcDto ->
        funcRepo.findById(funcDto.getName())
          .flatMap(fn -> {
            if (fn==null) {
              fn = oaasMapper.toFunc(funcDto);
            } else {
              if (!update) {
                throw new NoStackException("Function with this name already exist.")
                  .setCode(HttpResponseStatus.CONFLICT.code());
              }
              oaasMapper.set(funcDto, fn);
            }
            var cls = session.getReference(OaasClass.class,
              funcDto.getOutputCls());
            fn.setOutputCls(cls);
            return funcRepo.persist(fn);
          })
          .call(funcRepo::flush)
          .map(oaasMapper::toFunc)
      )
      .collect().asList();
  }

  @ReactiveTransactional
  public Uni<List<OaasFunctionDto>> createByYaml(boolean update, String body) {
    try {
      var funcs = yamlMapper.readValue(body, OaasFunctionDto[].class);
      return create(update, Arrays.asList(funcs));
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
