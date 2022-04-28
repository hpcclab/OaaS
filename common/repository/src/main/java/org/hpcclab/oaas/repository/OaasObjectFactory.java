package org.hpcclab.oaas.repository;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OaasObjectFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger( OaasObjectFactory.class );

  public OaasObject createBase(ObjectConstructRequest construct,
                               OaasClass cls,
                               String id) {
    var obj = OaasObject.createFromClasses(cls);
    obj.setId(id);
    obj.setEmbeddedRecord(construct.getEmbeddedRecord());
    obj.setLabels(construct.getLabels());
    obj.setOrigin(new OaasObjectOrigin().setRootId(id));
    obj.getState().setOverrideUrls(construct.getOverrideUrls());
    return obj;
  }

  public OaasObject createOutput(FunctionExecContext ctx) {
    var cls = ctx.getOutputCls();
    var source = ctx.getMain();
    OaasFunctionBinding binding = ctx.getBinding();
    var obj = OaasObject.createFromClasses(cls);

    if (source.getEmbeddedRecord() != null) {
      var forwardRecords = binding.getForwardRecords();
      if (forwardRecords!=null && !forwardRecords.isEmpty()) {
        JsonObject jo = new JsonObject(source.getEmbeddedRecord());
        var m = Maps.mutable.ofMap(jo.getMap());
        m.removeIf((k,v) -> !forwardRecords.contains(k));
        obj.setEmbeddedRecord(Json.encode(m));
      }
    }
    obj.setOrigin(ctx.createOrigin());
    return obj;
  }
}
