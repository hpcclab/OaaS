package org.hpcclab.oaas.taskmanager.factory;

import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.OaasObjectState;
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
    var dac = new DataAccessContext(taskContext);
    var b64Dac = contentUrlGenerator.genBase64Dac(dac);

    var task = new OaasTask();
    task.setId(taskContext.getOutput().getId());
    task.setMain(taskContext.getMain());
    task.setOutput(taskContext.getOutput());
    task.setFunction(taskContext.getFunction());
    task.setInputs(taskContext.getInputs());

    task.setMainKeys(genUrls(taskContext.getMain(), taskContext.getMainRefs(), b64Dac));
    var list = new ArrayList<Map<String,String>>();
    for (int i = 0; i < taskContext.getInputs().size(); i++) {
      list.add(genUrls(
        taskContext.getInputs().get(i),
        null,
        b64Dac
      ));
    }
    task.setInputKeys(list);
    task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getOutput().getId(), b64Dac));
    return task;
  }



  public Map<String, String> genUrls(OaasObject obj,
                                     Map<String, OaasObject> refs,
                                     String b64Dac) {
    Map<String, String> m = new HashMap<>();
    generateUrls(m, obj, refs, b64Dac, "");
    return m;
  }

  private void generateUrls(Map<String,String> map,
                            OaasObject obj,
                            Map<String, OaasObject> refs,
                            String b64Dac,
                            String prefix) {
    var cls = clsRepo.get(obj.getCls());
    if (cls.getStateType()!=OaasObjectState.StateType.COLLECTION) {
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
          b64Dac,
          prefix + entry.getKey() + ".");
      }
    }
  }
}
