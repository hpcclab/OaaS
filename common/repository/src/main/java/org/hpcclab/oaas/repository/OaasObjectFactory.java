package org.hpcclab.oaas.repository;

import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.function.FunctionExecContext;
import org.hpcclab.oaas.model.function.FunctionBinding;
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
    status.setCreatedTs(System.currentTimeMillis());
    obj.setStatus(status);
    return obj;
  }

  public OaasObject createOutput(FunctionExecContext ctx) {
    var cls = ctx.getOutputCls();
    var source = ctx.getMain();
    FunctionBinding binding = ctx.getBinding();
    var obj = OaasObject.createFromClasses(cls);

    if (source.getEmbeddedRecord() != null) {
      var node = source.getEmbeddedRecord();
      var forwardRecords = binding.getForwardRecords();
      if (forwardRecords!=null && !forwardRecords.isEmpty()) {
        node = node.deepCopy();
        var keys = Lists.mutable.ofAll(node::fieldNames);
        keys.removeAllIterable(forwardRecords);
        node.remove(keys);
      }
      obj.setEmbeddedRecord(node);
    }
    obj.setOrigin(ctx.createOrigin());
    obj.setId(idGenerator.generate(ctx));
    var status = new ObjectStatus();
    status.setCreatedTs(System.currentTimeMillis());
    obj.setStatus(status);
    return obj;
  }
}
