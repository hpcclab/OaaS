package org.hpcclab.msc.object.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.function.MscFunction;
import org.hpcclab.msc.object.model.NoStackException;
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

  public Uni<List<MscFunction>> list() {
    return funcRepo.listAll();
  }

  public Uni<MscFunction> create(MscFunction mscFunction) {
    return funcRepo.findByName(mscFunction.getName())
      .flatMap(fn -> {
        if (fn != null) {
          throw new NoStackException("Function with this name already exist.")
            .setCode(HttpResponseStatus.CONFLICT.code());
        }
        return funcRepo.persist(mscFunction);
      });
  }

  @Override
  public Uni<MscFunction> createByYaml(String body) {
    try {
      var func = mapper.readValue(body, MscFunction.class);
      return create(func);
    } catch (JsonProcessingException e) {
      throw new BadRequestException(e);
    }
  }

  public Uni<MscFunction> get(String funcName) {
    return funcRepo.findByName(funcName)
      .invoke(f -> {
        if (f==null)
          throw new NotFoundException();
      });
  }
}
