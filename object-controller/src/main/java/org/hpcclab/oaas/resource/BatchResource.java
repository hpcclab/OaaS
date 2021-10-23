package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestScoped
public class BatchResource implements BatchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchResource.class);

  @Inject
  OaasMapper oaasMapper;
  @Inject
  Mutiny.Session session;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  @ReactiveTransactional
  public Uni<Batch> create(Batch batch) {
    var classes = oaasMapper.toClass(batch.getClasses());
    var classMap = classes.stream().collect(Collectors.toMap(OaasClass::getName, Function.identity()));
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
    return session.persistAll(classes.toArray())
      .flatMap(v -> session.persistAll(functions.toArray()))
      .call(session::flush)
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
