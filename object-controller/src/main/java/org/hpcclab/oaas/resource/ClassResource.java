package org.hpcclab.oaas.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.mapper.OaasMapper;
import org.hpcclab.oaas.model.cls.OaasClassDto;
import org.hpcclab.oaas.model.proto.OaasClassPb;
import org.hpcclab.oaas.repository.IfnpOaasClassRepository;
import org.hpcclab.oaas.iface.service.ClassService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;

@ApplicationScoped
public class ClassResource implements ClassService {
  private static final Logger LOGGER = LoggerFactory.getLogger( ClassResource.class );
  @Inject
  IfnpOaasClassRepository classRepo;
  @Inject
  OaasMapper oaasMapper;
  ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());


  public Uni<List<OaasClassPb>> list(Integer page, Integer size) {
    if (page == null) page = 0;
    if (size == null) size = 100;
    var list = classRepo.pagination(page, size);
    return Uni.createFrom().item(list);
  }

  @Override
  public Uni<OaasClassPb> create(boolean update, OaasClassPb cls) {
    cls.validate();
    return classRepo.persist(cls);
  }

  @Override
  public Uni<OaasClassPb> patch(String name, OaasClassPb clsPatch) {
    return classRepo.getAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new)
      .flatMap(cls -> {
        oaasMapper.set(clsPatch, cls);
        cls.validate();
        return classRepo.persist(cls);
      });
  }

  @Override
  public Uni<OaasClassPb> createByYaml(boolean update, String body) {
    try {
      var cls = yamlMapper.readValue(body, OaasClassPb.class);
      return create(update, cls);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  @Override
  public Uni<OaasClassPb> get(String name) {
    return classRepo.getAsync(name)
      .onItem().ifNull().failWith(NotFoundException::new);
  }

  @Override
  public Uni<OaasClassPb> delete(String name) {
    return classRepo.remove(name)
      .onItem().ifNull().failWith(NotFoundException::new);
  }
}
