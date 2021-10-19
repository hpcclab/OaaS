package org.hpcclab.msc.object.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.mapper.OaasMapper;
import org.hpcclab.msc.object.model.OaasFunctionDto;
import org.hpcclab.msc.object.repository.OaasClassRepository;
import org.hpcclab.msc.object.repository.OaasFuncRepository;
import org.hpcclab.msc.object.service.BatchService;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.Arrays;

public class BatchResource implements BatchService {

  @Inject
  OaasFuncRepository funcRepo;
  @Inject
  OaasClassRepository classRepo;
  @Inject
  OaasMapper oaasMapper;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  @ReactiveTransactional
  public Uni<Batch> create(Batch batch) {
    var functions = oaasMapper.toFunc(batch.getFunctions());
    var classes = oaasMapper.toClass(batch.getClasses());
    return Uni.combine().all().unis(
      funcRepo.persist(functions),
      classRepo.persist(classes)
    ).combinedWith(l -> batch);
  }

  @Override
  public Uni<Batch> createByYaml(String body) {
    try {
      var batch = yamlMapper.readValue(body, Batch.class);
      return create(batch);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }
}
