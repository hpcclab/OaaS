package org.hpcclab.oaas.invocation.applier.logical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.OObjectFactory;
import org.hpcclab.oaas.invocation.applier.LogicalSubApplier;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;

import java.util.List;
import java.util.Map;

public class NewSubApplier implements LogicalSubApplier {
  DataUrlAllocator allocator;
  OObjectFactory objectFactory;
  ClassRepository clsRepo;
  ObjectRepoManager objRepoManager;
  ObjectMapper mapper;

  public NewSubApplier(DataUrlAllocator allocator,
                       OObjectFactory objectFactory,
                       ClassRepository clsRepo,
                       ObjectRepoManager objRepoManager,
                       ObjectMapper mapper) {
    this.allocator = allocator;
    this.objectFactory = objectFactory;
    this.clsRepo = clsRepo;
    this.objRepoManager = objRepoManager;
    this.mapper = mapper;
  }

  @Override
  public void validate(InvocationContext context) {
  }

  @Override
  public Uni<InvocationContext> apply(InvocationContext context) {
    var body = context.getRequest().body();
    ObjectConstructRequest req;
    if (body==null) {
      req = new ObjectConstructRequest();
    } else {
      try {
        req = mapper.treeToValue(body, ObjectConstructRequest.class);
      } catch (JsonProcessingException e) {
        throw new FunctionValidationException("Cannot decode body to 'ObjectConstructRequest'", e);
      }
    }
    if (context.getRequest().cls()!=null)
      req.setCls(context.getRequest().cls());
    return construct(context, req)
      .map(resp -> {
        context.setOutput(resp.getObject());
        resp.setObject(null);
        context.setRespBody(mapper.valueToTree(resp));
        return context;
      });
  }

  public Uni<ObjectConstructResponse> construct(InvocationContext ctx,
                                                ObjectConstructRequest construction) {
    var cls = clsRepo.get(construction.getCls());
    if (cls==null) throw StdOaasException.notFoundCls400(construction.getCls());
    return switch (cls.getObjectType()) {
      case SIMPLE, COMPOUND -> constructSimple(ctx, construction, cls);
    };
  }


  private Uni<ObjectConstructResponse> constructSimple(InvocationContext context,
                                                       ObjectConstructRequest construction,
                                                       OClass cls) {
    var obj = objectFactory.createBase(construction, cls,
      objectFactory.newId(context));
    linkReference(construction, obj, cls);
    var stateSpec = cls.getStateSpec();
    if (stateSpec==null) return objRepoManager.persistAsync(obj)
      .map(ignore -> new ObjectConstructResponse(obj, Map.of()));

    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> construction.getKeys().contains(k.getName()));
    if (ks.isEmpty()) {
      return objRepoManager.persistAsync(obj)
        .map(ignored -> new ObjectConstructResponse(obj, Map.of()));
    }
    DataAllocateRequest request = new DataAllocateRequest(obj.getId(), ks, cls.getStateSpec().getDefaultProvider(), true);
    return allocator.allocate(List.of(request))
      .map(list -> new ObjectConstructResponse(obj, list.get(0).getUrlKeys()))
      .call(() -> objRepoManager.persistAsync(obj));
  }


  private void linkReference(ObjectConstructRequest request,
                             OObject obj,
                             OClass cls) {
    //TODO validate the references of request
    obj.setRefs(request.getRefs());
  }

  @Override
  public String functionKey() {
    return "builtin.logical.new";
  }
}
