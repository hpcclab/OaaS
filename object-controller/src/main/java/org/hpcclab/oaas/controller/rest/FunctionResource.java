package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.OcConfig;
import org.hpcclab.oaas.iface.service.FunctionService;
import org.hpcclab.oaas.controller.mapper.CtxMapper;
import org.hpcclab.oaas.model.Pagination;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.controller.service.FunctionProvisionPublisher;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;

@RequestScoped
public class FunctionResource implements FunctionService {
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OcConfig config;
  @Inject
  FunctionProvisionPublisher provisionPublisher;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Blocking
  public Pagination<OaasFunction> list(Long offset, Integer limit) {
    if (offset== null) offset = 0L;
    if (limit== null) limit = 20;
    return funcRepo.pagination(offset, limit);
  }

  public Uni<List<OaasFunction>> create(boolean update, List<OaasFunction> functionDtos) {
    var uni = Multi.createFrom().iterable(functionDtos)
      .onItem()
      .transformToUniAndConcatenate(funcDto -> funcRepo.persist(funcDto)
      )
      .collect().asList();
    if (config.kafkaEnabled()) {
      return uni
        .call(functions -> provisionPublisher.submitNewFunction(functions.stream()));
    } else {
      return uni;
    }
  }

  public Uni<List<OaasFunction>> createByYaml(boolean update, String body) {
    try {
      var funcs = yamlMapper.readValue(body, OaasFunction[].class);
      return create(update, Arrays.asList(funcs));
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<OaasFunction> get(String funcName) {
    return funcRepo.getAsync(funcName)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
