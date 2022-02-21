package org.hpcclab.oaas.taskmanager.factory;

import org.apache.kafka.common.protocol.types.Field;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.proto.OaasObject;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.hpcclab.oaas.taskmanager.service.ContentUrlGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TaskFactory {
  @Inject
  ContentUrlGenerator contentUrlGenerator;

  public OaasTask genTask(TaskContext taskContext) {
    var dac = new DataAccessContext(taskContext);
    var b64Dac = contentUrlGenerator.genBase64Dac(dac);

    var task = new OaasTask();
    task.setId(taskContext.getOutput().getId().toString());
    task.setMain(taskContext.getMain());
    task.setOutput(taskContext.getOutput());
    task.setFunction(taskContext.getFunction());
    task.setInputs(taskContext.getInputs());

    task.setMainKeys(genUrls(taskContext.getMain(),taskContext.getMainCls(), b64Dac));
    var list = new ArrayList<Map<String,String>>();
    for (int i = 0; i < taskContext.getInputs().size(); i++) {
      list.add(genUrls(
        taskContext.getInputs().get(i),
        taskContext.getInputCls().get(i),
        b64Dac
      ));
    }
    task.setInputKeys(list);
    task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getOutput().getId(), b64Dac));
    return task;
  }

  public Map<String, String> genUrls(OaasObject obj,
                                     OaasClass cls,
                                     String b64Dac) {
    if (cls.getStateType() != OaasObjectState.StateType.COLLECTION) {
      Map<String, String> m = new HashMap<>();
      for (KeySpecification keySpec : cls.getStateSpec().getKeySpecs()) {
        var url  =
          contentUrlGenerator.generateUrl(obj.getId(),keySpec.getName(),b64Dac);
        m.put(keySpec.getName(), url);
      }
      if (obj.getState().getOverrideUrls() != null)
        m.putAll(obj.getState().getOverrideUrls());
      return m;
    } else {
      if (obj.getState().getOverrideUrls() == null)
        return Map.of();
      return obj.getState().getOverrideUrls();
    }
  }
}
