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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BatchResource implements BatchService {
private static final Logger LOGGER = LoggerFactory.getLogger( BatchResource.class );
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
    return funcRepo.getSession()
      .flatMap(session -> {
        var classes = oaasMapper.toClass(batch.getClasses());
        var classMap = classes.stream().collect(Collectors.toMap(OaasClass::getName, Function.identity()));
        LOGGER.info("cls {}", classMap);
        var functions = batch.getFunctions().stream()
          .map(fd -> {
            var func = oaasMapper.toFunc(fd);

            if (fd.getOutputCls()!=null) {
              if (classMap.containsKey(fd.getOutputCls())) {
                func.setOutputCls(classMap.get(fd.getOutputCls()));
              } else {
                func.setOutputCls(session.getReference(OaasClass.class, fd.getOutputCls()));
              }
            } else {
              func.setOutputCls(null);
            }
            return func;
          })
          .toList();
        LOGGER.info("functions {}", functions);
        return session.persistAll(classes.toArray())
          .flatMap(v -> session.persistAll(functions.toArray()))
          .call(session::flush);
      })
      .map(l -> batch);
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
