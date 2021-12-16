package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.iface.service.BatchService;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.repository.IfnpOaasClassRepository;
import org.hpcclab.oaas.repository.IfnpOaasFuncRepository;
import org.hpcclab.oaas.service.FunctionProvisionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class BatchResource implements BatchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchResource.class);

  @Inject
  IfnpOaasClassRepository classRepo;
  @Inject
  IfnpOaasFuncRepository funcRepo;
  @Inject
  FunctionProvisionPublisher provisionPublisher;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  public Uni<Batch> create(Batch batch) {
    return classRepo.persist(batch.getClasses())
      .flatMap(ignore -> funcRepo.persist(batch.getFunctions()))
      .call(functions -> provisionPublisher.submitNewFunction(batch.getFunctions().stream()))
      .replaceWith(batch);
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
