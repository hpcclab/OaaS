package org.hpcclab.oaas.invocation.controller.fn.logical;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.fn.AbstractFunctionController;
import org.hpcclab.oaas.invocation.controller.fn.BuiltinFunctionController;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.repository.ObjectRepoManager;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.id.IdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class NativeQueryFnController extends AbstractFunctionController implements BuiltinFunctionController {
  ObjectRepository repo;
  String query;
  final ObjectRepoManager repoManager;

  public NativeQueryFnController(IdGenerator idGenerator,
                                 ObjectMapper mapper,
                                 ObjectRepoManager repoManager) {
    super(idGenerator, mapper);
    this.repoManager = repoManager;
  }

  @Override
  protected void afterBind() {
    repo = repoManager.getOrCreate(cls);
    query = customConfig.getString("query");
    if (query == null)
      throw StdOaasException.format("query must be defined");
  }

  @Override
  protected void validate(InvocationCtx ctx) {
    try {
      repo.getQueryService();
    } catch (UnsupportedOperationException e){
      throw new StdOaasException("Native query is not supported", 501);
    }
  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    Map<String, Object> finalArgs = new HashMap<>(functionBinding.getDefaultArgs());
    if (ctx.getArgs() != null) finalArgs.putAll(ctx.getArgs());
    Uni<List<GOObject>> uni = repo.getQueryService()
      .queryAsync(query, finalArgs);
    return uni
      .map(list -> ctx.setRespBody(
        mapper.valueToTree(Map.of("items", list)))
      );
  }


  @Override
  public String getFnKey() {
    return "builtin.native-query";
  }
}
