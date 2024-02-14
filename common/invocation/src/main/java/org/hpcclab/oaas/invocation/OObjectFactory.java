package org.hpcclab.oaas.invocation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.invocation.applier.logical.ObjectConstructRequest;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.repository.id.IdGenerator;

@ApplicationScoped
public class OObjectFactory {
  IdGenerator idGenerator;

  @Inject
  public OObjectFactory(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  public OObject createBase(ObjectConstructRequest construct,
                            OClass cls,
                            String id) {
    var obj = OObject.createFromClasses(cls);
    obj.setId(id);
    obj.setData(construct.getData());
    obj.getState().setOverrideUrls(construct.getOverrideUrls());
    var state = new OaasObjectState();
    if (cls.getStateType()!=StateType.COLLECTION) {
      var verIds = Lists.fixedSize.ofAll(cls.getStateSpec().getKeySpecs())
        .toMap(KeySpecification::getName, __ -> id);
      state.setVerIds(DSMap.wrap(verIds));
    }
    obj.setState(state);
    obj.setRevision(1);
    return obj;
  }

  public OObject createOutput(InvocationContext ctx) {
    var cls = ctx.getOutputCls();
    var source = ctx.getMain();
    var obj = OObject.createFromClasses(cls);
    obj.setData(source.getData());
    obj.setId(idGenerator.generate(ctx));
    obj.setRevision(0);
    return obj;
  }

  public String newId(InvocationContext ctx){
    return idGenerator.generate(ctx);
  }
  public String newId(){
    return idGenerator.generate();
  }
}
