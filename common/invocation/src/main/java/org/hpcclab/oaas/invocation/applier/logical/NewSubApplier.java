package org.hpcclab.oaas.invocation.applier.logical;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.invocation.DataUrlAllocator;
import org.hpcclab.oaas.invocation.applier.LogicalSubApplier;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.exception.FunctionValidationException;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.object.ObjectConstructResponse;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.invocation.OaasObjectFactory;
import org.hpcclab.oaas.repository.ObjectRepository;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NewSubApplier implements LogicalSubApplier {
  @Inject
  DataUrlAllocator allocator;
  @Inject
  OaasObjectFactory objectFactory;
  @Inject
  ClassRepository clsRepo;
  @Inject
  ObjectRepository objRepo;
  @Inject
  ObjectMapper mapper;

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
    return construct(req)
      .map(resp -> {
        context.setOutput(resp.getObject());
        resp.setObject(null);
        context.setRespBody(mapper.valueToTree(resp));
        return context;
      });
  }

  @Override
  public String functionKey() {
    return "builtin.logical.new";
  }

  public Uni<ObjectConstructResponse> construct(ObjectConstructRequest construction) {
    var cls = clsRepo.get(construction.getCls());
    if (cls==null) throw StdOaasException.notFoundCls400(construction.getCls());
    return switch (cls.getObjectType()) {
      case SIMPLE, COMPOUND -> constructSimple(construction, cls);
    };
  }

  private void linkReference(ObjectConstructRequest request,
                             OaasObject obj,
                             OaasClass cls) {
    //TODO validate the references of request
    obj.setRefs(request.getRefs());
  }

  private Uni<ObjectConstructResponse> constructSimple(ObjectConstructRequest construction,
                                                       OaasClass cls) {
    var obj = objectFactory.createBase(construction, cls);
    var async = objRepo.async();
    linkReference(construction, obj, cls);
    var stateSpec = cls.getStateSpec();
    if (stateSpec==null) return async.persistAsync(obj)
      .map(ignore -> new ObjectConstructResponse(obj, Map.of()));

    var ks = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
      .select(k -> construction.getKeys().contains(k.getName()));
    if (ks.isEmpty()) {
      return async.persistAsync(obj)
        .map(ignored -> new ObjectConstructResponse(obj, Map.of()));
    }
    DataAllocateRequest request = new DataAllocateRequest(obj.getId(), ks, cls.getStateSpec().getDefaultProvider(), true);
    return allocator.allocate(List.of(request))
      .map(list -> new ObjectConstructResponse(obj, list.get(0).getUrlKeys()))
      .call(() -> async.persistAsync(obj));
  }
}
