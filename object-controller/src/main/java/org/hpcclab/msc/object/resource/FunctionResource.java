package org.hpcclab.msc.object.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.OaasFunction;
import org.hpcclab.msc.object.exception.NoStackException;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.service.FunctionService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;

@ApplicationScoped
public class FunctionResource implements FunctionService {
  @Inject
  MscFuncRepository funcRepo;
  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  public Uni<List<OaasFunction>> list() {
    return funcRepo.listAll();
  }

  public Uni<OaasFunction> create(boolean update, OaasFunction function) {
    if (update) {
      return funcRepo.persistOrUpdate(function);
    }
    return funcRepo.findByName(function.getName())
      .flatMap(fn -> {
        if (fn != null) {
          throw new NoStackException("Function with this name already exist.")
            .setCode(HttpResponseStatus.CONFLICT.code());
        }
        return funcRepo.persist(function);
      });
  }

  @Override
  public Uni<OaasFunction> createByYaml(boolean update, String body) {
    try {
      var func = mapper.readValue(body, OaasFunction.class);
      return create(update, func);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<OaasFunction> get(String funcName) {
    return funcRepo.findByName(funcName)
      .invoke(f -> {
        if (f==null)
          throw new NotFoundException();
      });
  }
}
