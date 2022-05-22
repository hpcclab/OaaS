package org.hpcclab.oaas.controller.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.controller.OcConfig;
import org.hpcclab.oaas.iface.service.BatchService;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.repository.impl.OaasClassRepository;
import org.hpcclab.oaas.repository.impl.OaasFuncRepository;
import org.hpcclab.oaas.controller.service.FunctionProvisionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class BatchResource implements BatchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchResource.class);

  @Inject
  OaasClassRepository classRepo;
  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  FunctionProvisionPublisher provisionPublisher;
  @Inject
  OcConfig config;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public Uni<Batch> create(Batch batch) {
    var classes = batch.getClasses();
    var functions = batch.getFunctions();
    for (OaasClass cls : classes) {
      cls.validate();
    }
    for (OaasFunction function : functions) {
      function.validate();
    }
    var uni = classRepo.persistAsync(classes)
      .flatMap(ignore -> funcRepo.persistAsync(functions));
    if (config.kafkaEnabled()) {
      return uni.call(ignored -> provisionPublisher.submitNewFunction(batch.getFunctions().stream()))
        .replaceWith(batch);
    } else {
      return uni.replaceWith(batch);
    }
  }

  @Override
  public Uni<Batch> createByYaml(String body) {
    try {
      var batch = yamlMapper.readValue(body, Batch.class);
      return create(batch);
    } catch (JsonProcessingException e) {
      throw new NoStackException(e.getMessage(), 400);
    }
  }
}
