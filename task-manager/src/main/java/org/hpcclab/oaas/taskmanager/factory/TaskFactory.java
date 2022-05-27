package org.hpcclab.oaas.taskmanager.factory;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.repository.impl.OaasClassRepository;
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
  @Inject
  OaasClassRepository clsRepo;

  public OaasTask genTask(TaskContext taskContext) {
    var cls = clsRepo.get(taskContext.getOutput().getCls());

    var task = new OaasTask();
    task.setId(taskContext.getOutput().getId());
    task.setMain(taskContext.getMain());
    task.setOutput(taskContext.getOutput());
    task.setFunction(taskContext.getFunction());
    task.setInputs(taskContext.getInputs());

    task.setMainKeys(genUrls(taskContext.getMain(), cls, taskContext.getMainRefs()));
    var list = new ArrayList<Map<String,String>>();
    for (int i = 0; i < taskContext.getInputs().size(); i++) {
      list.add(genUrls(
        taskContext.getInputs().get(i),
        cls,
        null
      ));
    }
    task.setInputKeys(list);
    if (cls.getStateType() == StateType.COLLECTION ||
      !cls.getStateSpec().getKeySpecs().isEmpty()) {
      var b64Dac = DataAccessContext.generate(task.getOutput(),cls)
        .encode();
      task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getOutput().getId(), b64Dac));
    }
    task.setTs(System.currentTimeMillis());
    return task;
  }



  public Map<String, String> genUrls(OaasObject obj,
                                     OaasClass cls,
                                     Map<String, OaasObject> refs) {
    Map<String, String> m = new HashMap<>();
    generateUrls(m, obj, cls, refs, "");
    return m;
  }

  private void generateUrls(Map<String,String> map,
                            OaasObject obj,
                            OaasClass cls,
                            Map<String, OaasObject> refs,
//                            String b64Dac,
                            String prefix) {
    if (cls == null) {
      cls = clsRepo.get(obj.getCls());
    }
    var dac = DataAccessContext.generate(obj, cls);
    var b64Dac = dac.encode();
    if (cls.getStateType()!=StateType.COLLECTION) {
      for (KeySpecification keySpec : cls.getStateSpec().getKeySpecs()) {
        var url  =
          contentUrlGenerator.generateUrl(obj.getId(),keySpec.getName(),b64Dac);
        map.put(prefix + keySpec.getName(), url);
      }
    } else {
      var url  =
        contentUrlGenerator.generateUrl(obj.getId(),"",b64Dac);
      map.put(prefix + '*', url);
    }

    if (obj.getState().getOverrideUrls() != null) {
      obj.getState().getOverrideUrls()
        .forEach((k,v) -> map.put(prefix + k, v));
    }
    if (refs != null) {
      for (var entry: refs.entrySet()) {
        generateUrls(
          map,
          entry.getValue(),
          null,
          null,
          prefix + entry.getKey() + ".");
      }
    }
  }
}
