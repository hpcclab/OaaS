package org.hpcclab.msc.object.resource;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.model.ErrorMessage;
import org.hpcclab.msc.object.model.NoStackException;
import org.hpcclab.msc.object.repository.MscFuncRepository;
import org.hpcclab.msc.object.service.FunctionService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
public class FunctionResource implements FunctionService {
  @Inject
  MscFuncRepository funcRepo;

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

  public Uni<MscFunction> get(String funcName) {
    return funcRepo.findByName(funcName)
      .invoke(f -> {
        if (f==null)
          throw new NotFoundException();
      });
  }
}
