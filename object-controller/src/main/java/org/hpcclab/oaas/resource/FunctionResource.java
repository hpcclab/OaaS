package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.cache.CacheResult;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hibernate.CacheMode;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.iface.service.FunctionService;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.OaasFunctionDto;
import org.hpcclab.oaas.model.proto.OaasFunctionPb;
import org.hpcclab.oaas.repository.IfnpOaasFuncRepository;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
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

  public Uni<List<OaasFunctionPb>> list(Integer page, Integer size) {
    if (page == null) page = 0;
    if (size == null) size = 100;
    var list = funcRepo.pagination(page, size);
    return Uni.createFrom().item(list);
  }

  @ReactiveTransactional
  public Uni<List<OaasFunctionPb>> create(boolean update, List<OaasFunctionPb> functionDtos) {
    return Multi.createFrom().iterable(functionDtos)
      .onItem()
      .transformToUniAndConcatenate(funcDto -> funcRepo.persist(funcDto)
      )
      .collect().asList()
      .call(functions -> provisionPublisher.submitNewFunction(functions.stream()));
  }

//  @ReactiveTransactional
  public Uni<List<OaasFunctionPb>> createByYaml(boolean update, String body) {
    try {
      var funcs = yamlMapper.readValue(body, OaasFunctionPb[].class);
      return create(update, Arrays.asList(funcs));
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<OaasFunctionPb> get(String funcName) {
    return funcRepo.getAsync(funcName)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
