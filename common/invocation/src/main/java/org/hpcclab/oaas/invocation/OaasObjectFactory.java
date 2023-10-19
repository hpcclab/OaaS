package org.hpcclab.oaas.invocation;

import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.object.ObjectConstructRequest;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.object.ObjectStatus;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.stream.Collectors;

@ApplicationScoped
public class OaasObjectFactory {
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
    obj.setData(construct.getData());
    obj.getState().setOverrideUrls(construct.getOverrideUrls());
    var status = new ObjectStatus();
    obj.setStatus(status);
    var state = new OaasObjectState();
    if (cls.getStateType() != StateType.COLLECTION) {
      var verIds = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
          .toMap(KeySpecification::getName, __ -> id);
      state.setVerIds(DSMap.wrap(verIds));
    }
    obj.setState(state);
    obj.setRevision(1);
    return obj;
  }

  public OaasObject createOutput(InvocationContext ctx) {
    var cls = ctx.getOutputCls();
    var source = ctx.getMain();
    FunctionBinding binding = ctx.getFb();
    var obj = OaasObject.createFromClasses(cls);

    if (source.getData() != null) {
      var node = source.getData();
      var forwardRecords = binding.getForwardRecords();
      if (forwardRecords!=null && !forwardRecords.isEmpty()) {
        node = node.deepCopy();
        var keys = Lists.mutable.ofAll(node::fieldNames);
        keys.removeAllIterable(forwardRecords);
        node.remove(keys);
      }
      obj.setData(node);
    }
    obj.setId(idGenerator.generate(ctx));
    var status = new ObjectStatus();
    obj.setStatus(status);
    obj.setRevision(0);
    return obj;
  }
}
