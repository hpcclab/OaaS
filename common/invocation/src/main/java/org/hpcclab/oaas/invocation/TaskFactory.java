package org.hpcclab.oaas.invocation;

import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskContext;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TaskFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskFactory.class );
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
    var verId = idGenerator.generate();
    taskContext.setVId(verId);
    var mainCls = clsRepo.get(taskContext.getMain().getCls());

    var task = new OaasTask();
    task.setId(new TaskIdentity(taskContext).encode());
    task.setPartKey(taskContext.getMain().getId());
    task.setVId(verId);
    task.setFbName(taskContext.getFbName());
    task.setMain(taskContext.getMain());
    task.setFuncKey(taskContext.getFuncKey());
    task.setFunction(taskContext.getFunction());
    task.setInputs(taskContext.getInputs());
    task.setImmutable(taskContext.isImmutable());
    var binding = mainCls.findFunction(taskContext.getFbName());
    task.setArgs(taskContext.resolveArgs(binding));

    if (taskContext.getOutput()!=null) {
      task.setOutput(taskContext.getOutput());

      var outCls = clsRepo.get(taskContext.getOutput().getCls());

      if (outCls.getStateType()==StateType.COLLECTION ||
        !outCls.getStateSpec().getKeySpecs().isEmpty()) {
        var b64Dac = DataAccessContext.generate(task.getOutput(), AccessLevel.ALL, verId)
          .encode();
        task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getOutput().getId(), b64Dac));
      }
    }

    if (taskContext.getFunction().getType().isMutable()) {
      var b64Dac = DataAccessContext.generate(task.getMain(), AccessLevel.ALL, verId)
        .encode();
      task.setAllocMainUrl(contentUrlGenerator.generateAllocateUrl(taskContext.getMain().getId(), b64Dac));
    }
    task.setMainKeys(genUrls(taskContext.getMain(), taskContext.getMainRefs()));

    var inputContextKeys = new ArrayList<String>();
    for (int i = 0; i < taskContext.getInputs().size(); i++) {
      var inputObj = taskContext.getInputs().get(i);
      AccessLevel level = mainCls.isSamePackage(inputObj.getCls()) ?
        AccessLevel.INTERNAL:AccessLevel.INVOKE_DEP;
      var b64Dac = DataAccessContext.generate(inputObj, level).encode();
      inputContextKeys.add(b64Dac);
    }
    task.setInputContextKeys(inputContextKeys);

    task.setTs(System.currentTimeMillis());
    return task;
  }


  public Map<String, String> genUrls(OaasObject obj,
                                     Map<String, OaasObject> refs) {
    Map<String, String> m = new HashMap<>();
    generateUrls(m, obj, refs, "");
    return m;
  }

  private void generateUrls(Map<String, String> map,
                            OaasObject obj,
                            Map<String, OaasObject> refs,
                            String prefix) {
    var dac = DataAccessContext.generate(obj);
    var b64Dac = dac.encode();

    var verIds = obj.getState().getVerIds();
    if (verIds!=null && !verIds.isEmpty()) {
      for (var vidEntry : verIds) {
        var url =
          contentUrlGenerator.generateUrl(obj.getId(), vidEntry.getValue(),
            vidEntry.getKey(), b64Dac);
        map.put(prefix + vidEntry.getKey(), url);
      }
    }

    if (obj.getState().getOverrideUrls()!=null) {
      obj.getState().getOverrideUrls()
        .forEach(e -> map.put(prefix + e.getKey(), e.getValue()));
    }
    if (refs!=null) {
      for (var entry : refs.entrySet()) {
        generateUrls(
          map,
          entry.getValue(),
          null,
          prefix + entry.getKey() + ".");
      }
    }
  }
}
