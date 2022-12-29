package org.hpcclab.oaas.invocation;

import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.IdGenerator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TaskFactory {
  private final ContentUrlGenerator contentUrlGenerator;

  private final EntityRepository<String, OaasClass> clsRepo;

  private final IdGenerator idGenerator;

  @Inject
  public TaskFactory(ContentUrlGenerator contentUrlGenerator,
                     EntityRepository<String, OaasClass> clsRepo,
                     IdGenerator idGenerator) {
    this.contentUrlGenerator = contentUrlGenerator;
    this.clsRepo = clsRepo;
    this.idGenerator = idGenerator;
  }

  public OaasTask genTask(TaskContext taskContext) {
    var mainCls = clsRepo.get(taskContext.getMain().getCls());

    var task = new OaasTask();
    task.setId(taskContext.getMain().getId());
    task.setMain(taskContext.getMain());
    task.setFunction(taskContext.getFunction());
    task.setInputs(taskContext.getInputs());
    task.setArgs(taskContext.getArgs());

    if (taskContext.getOutput() != null) {
      task.setOutput(taskContext.getOutput());

      var outCls = clsRepo.get(taskContext.getOutput().getCls());

      if (outCls.getStateType()==StateType.COLLECTION ||
        !outCls.getStateSpec().getKeySpecs().isEmpty()) {
        var b64Dac = DataAccessContext.generate(task.getOutput())
          .encode();
        task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getOutput().getId(), b64Dac));
      }
    }

    task.setMainKeys(genUrls(taskContext.getMain(), mainCls, taskContext.getMainRefs()));

//    var list = new ArrayList<Map<String, String>>();
    var inputContextKeys = new ArrayList<String>();
    for (int i = 0; i < taskContext.getInputs().size(); i++) {
//      list.add(genUrls(
//        taskContext.getInputs().get(i),
//        null,
//        null
//      ));
      var inputObj = taskContext.getInputs().get(i);
      AccessLevel level = mainCls.isSamePackage(inputObj.getCls())?
        AccessLevel.INTERNAL: AccessLevel.INVOKE_DEP;
      var b64Dac = DataAccessContext.generate(inputObj, level).encode();
      inputContextKeys.add(b64Dac);
    }
//    task.setInputKeys(list);
    task.setInputContextKeys(inputContextKeys);

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

  private void generateUrls(Map<String, String> map,
                            OaasObject obj,
                            OaasClass cls,
                            Map<String, OaasObject> refs,
                            String prefix) {
    if (cls==null) {
      cls = clsRepo.get(obj.getCls());
    }
    var dac = DataAccessContext.generate(obj);
    var b64Dac = dac.encode();
    if (cls.getStateType()!=StateType.COLLECTION) {
      for (KeySpecification keySpec : cls.getStateSpec().getKeySpecs()) {
        var url =
          contentUrlGenerator.generateUrl(obj.getId(), keySpec.getName(), b64Dac);
        map.put(prefix + keySpec.getName(), url);
      }
    } else {
      var url =
        contentUrlGenerator.generateUrl(obj.getId(), "", b64Dac);
      map.put(prefix + '*', url);
    }

    if (obj.getState().getOverrideUrls()!=null) {
      obj.getState().getOverrideUrls()
        .forEach((k, v) -> map.put(prefix + k, v));
    }
    if (refs!=null) {
      for (var entry : refs.entrySet()) {
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
