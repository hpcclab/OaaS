package org.hpcclab.msc.object.resource;

import io.smallrye.mutiny.Uni;
import org.hpcclab.msc.object.entity.MscObject;
import org.hpcclab.msc.object.repository.MscObjectRepository;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/api/objects")
public class ObjectResource {
  @Inject
  MscObjectRepository objectRepo;

  @GET
  public Uni<List<MscObject>> list() {
    return objectRepo.listAll();
  }

  @POST
  public Uni<MscObject> createRoot() {
    return null;
  }


}
