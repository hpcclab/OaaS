package org.hpcclab.oaas.repository;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.OaasFunctionBinding;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectStatus;
import org.hpcclab.oaas.model.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class OaasObjectFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger( OaasObjectFactory.class );

  IdGenerator idGenerator;

  @Inject
  public OaasObjectFactory(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }
  public OaasObject createBase(ObjectConstructRequest construct,
                               OaasClass cls) {
    return createBase(construct,cls, idGenerator.generate(construct));
  }
  public OaasObject createBase(ObjectConstructRequest construct,
                               OaasClass cls,
                               String id) {
    var obj = OaasObject.createFromClasses(cls);
    obj.setId(id);
    obj.setEmbeddedRecord(construct.getEmbeddedRecord());
    obj.setLabels(construct.getLabels());
    obj.setOrigin(new ObjectOrigin());
    obj.getState().setOverrideUrls(construct.getOverrideUrls());
    var status = new ObjectStatus();
    status.setTaskStatus(TaskStatus.READY);
    status.setCreatedTime(System.currentTimeMillis());
    obj.setStatus(status);
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
    obj.setId(idGenerator.generate(ctx));
    var status = new ObjectStatus();
    status.setCreatedTime(System.currentTimeMillis());
    obj.setStatus(status);
    return obj;
  }
}
