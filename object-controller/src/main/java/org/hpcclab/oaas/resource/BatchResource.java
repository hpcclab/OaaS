package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.hibernate.reactive.panache.common.runtime.ReactiveTransactional;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hpcclab.oaas.entity.OaasClass;
import org.hpcclab.oaas.entity.function.OaasFunction;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.function.OaasFunctionBindingDto;
import org.hpcclab.oaas.repository.OaasClassRepository;
import org.hpcclab.oaas.repository.OaasFuncRepository;
import org.hpcclab.oaas.service.BatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.HashSet;

@RequestScoped
public class BatchResource implements BatchService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BatchResource.class);

  @Inject
  OaasMapper oaasMapper;
  @Inject
  Mutiny.Session session;
  @Inject
  OaasClassRepository classRepo;
  @Inject
  OaasFuncRepository funcRepo;

  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

  @Override
  @ReactiveTransactional
  public Uni<Batch> create(Batch batch) {
    return Multi.createFrom().iterable(batch.getClasses())
      .onItem().transformToUniAndConcatenate(clsDto -> classRepo.getDeep(clsDto.getName())
          .onItem().ifNotNull()
          .invoke(cls -> oaasMapper.set(clsDto, cls))
          .onItem().ifNull()
          .switchTo(() -> classRepo.persist(oaasMapper.toClass(clsDto)))
          .map(cls -> Tuple2.of(cls, clsDto))
      )
      .collect().asMap(t -> t.getItem1().getName())
      .flatMap(classMap ->
        Multi.createFrom()
          .iterable(batch.getFunctions())
          .onItem().transformToUniAndConcatenate(functionDto -> funcRepo.findById(functionDto.getName())
            .onItem().ifNotNull()
            .invoke(func -> oaasMapper.set(functionDto, func))
            .onItem().ifNull()
            .switchTo(() -> funcRepo.persist(oaasMapper.toFunc(functionDto)))
            .invoke(func -> {
              if (functionDto.getOutputCls()==null) {
                func.setOutputCls(null);
                return;
              }
              if (classMap.containsKey(functionDto.getOutputCls())) {
                func.setOutputCls(classMap.get(functionDto.getOutputCls()).getItem1());
              } else {
                func.setOutputCls(session.getReference(OaasClass.class, functionDto.getOutputCls()));
              }
            })
          )
          .collect()
          .asMap(OaasFunction::getName)
          .invoke(functionMap -> {
              for (var tuple : classMap.values()) {
                var clsDto = tuple.getItem2();
                var cls = tuple.getItem1();
                if (cls.getFunctions() == null) cls.setFunctions(new HashSet<>());
                cls.getFunctions().clear();
                for (OaasFunctionBindingDto bindingDto : clsDto.getFunctions()) {
                  var fb = new OaasFunctionBinding();
                  if (functionMap.containsKey(bindingDto.getFunction())) {
                    fb.setAccess(bindingDto.getAccess())
                      .setFunction(functionMap.get(bindingDto.getFunction()));
                  } else {
                    fb.setAccess(bindingDto.getAccess())
                      .setFunction(session.getReference(OaasFunction.class,
                        bindingDto.getFunction()));
                  }
                  cls.getFunctions().add(fb);
                }
              }
            }
          )
      )
      .map(ignore -> batch);
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
