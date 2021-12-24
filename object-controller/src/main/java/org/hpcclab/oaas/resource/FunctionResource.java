package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.iface.service.FunctionService;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.proto.OaasFunction;
import org.hpcclab.oaas.repository.IfnpOaasFuncRepository;
import org.hpcclab.oaas.service.FunctionProvisionPublisher;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;

@RequestScoped
public class FunctionResource implements FunctionService {
  @Inject
  IfnpOaasFuncRepository funcRepo;
  @Inject
  OaasMapper oaasMapper;
  @Inject
  FunctionProvisionPublisher provisionPublisher;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  public Uni<List<OaasFunction>> list(Integer page, Integer size) {
    if (page==null) page = 0;
    if (size==null) size = 100;
    var list = funcRepo.pagination(page, size);
    return Uni.createFrom().item(list);
  }

  public Uni<List<OaasFunction>> create(boolean update, List<OaasFunction> functionDtos) {
    return Multi.createFrom().iterable(functionDtos)
      .onItem()
      .transformToUniAndConcatenate(funcDto -> funcRepo.persist(funcDto)
      )
      .collect().asList()
      .call(functions -> provisionPublisher.submitNewFunction(functions.stream()));
  }

  //  @ReactiveTransactional
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
