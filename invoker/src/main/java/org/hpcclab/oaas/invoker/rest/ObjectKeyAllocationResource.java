package org.hpcclab.oaas.invoker.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.hpcclab.oaas.invocation.controller.ClassControllerRegistry;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.storage.S3Adapter;
import org.jboss.resteasy.reactive.RestQuery;

import java.util.Map;
import java.util.Set;

/**
 * @author Pawissanutt
 */
@Path("/api/allocate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class ObjectKeyAllocationResource {
  final ClassControllerRegistry registry;
  final S3Adapter s3Adapter;

  public ObjectKeyAllocationResource(ClassControllerRegistry registry, S3Adapter s3Adapter) {
    this.registry = registry;
    this.s3Adapter = s3Adapter;
  }

  @GET
  @Path("{oid}")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getAllocatedUrls(String oid,
                                              Set<String> allocateKeys,
                                              @RestQuery String contextKey) {
    var dac = DataAccessContext.parse(contextKey);
    var clsName = dac.getCls();
    var cls = registry.getClassController(clsName).getCls();
    if (cls==null) throw StdOaasException.notFoundCls400(clsName);
    var req = new DataAllocateRequest(oid,
      dac.getVid(),
      cls.getStateSpec().getKeySpecs().stream().map(KeySpecification::getName).toList(),
      null,
      false);
    return s3Adapter.allocate(req);
  }
}
