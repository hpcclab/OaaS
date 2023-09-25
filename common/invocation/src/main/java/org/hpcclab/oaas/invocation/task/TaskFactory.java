package org.hpcclab.oaas.invocation.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.invocation.InvocationContext;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskIdentity;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class TaskFactory {
  private static final Logger logger = LoggerFactory.getLogger(TaskFactory.class);
  private final ContentUrlGenerator contentUrlGenerator;


  private final IdGenerator idGenerator;

  @Inject
  public TaskFactory(ContentUrlGenerator contentUrlGenerator,
                     IdGenerator idGenerator) {
    this.contentUrlGenerator = contentUrlGenerator;
    this.idGenerator = idGenerator;
  }

  public OaasTask genTask(InvocationContext ctx) {
    var verId = ctx.initNode().getKey();
    var mainCls = ctx.getMainCls();

    var task = new OaasTask();
    task.setId(new TaskIdentity(ctx));
    task.setPartKey(ctx.getMain().getId());
    task.setFbName(ctx.getFbName());
    task.setMain(ctx.getMain());
    task.setFuncKey(ctx.getFuncKey());
    task.setFunction(ctx.getFunction());
    task.setInputs(ctx.getInputs());
    task.setImmutable(ctx.isImmutable());
    var binding = mainCls.findFunction(ctx.getFbName());
    task.setArgs(ctx.resolveArgs(binding));

    if (ctx.getOutput()!=null) {
      task.setOutput(ctx.getOutput());

      var outCls = ctx.getClsMap().get(ctx.getOutput().getCls());

      if (outCls.getStateType()==StateType.COLLECTION ||
        !outCls.getStateSpec().getKeySpecs().isEmpty()) {
        var dac = DataAccessContext.generate(task.getOutput(), AccessLevel.ALL, verId);
        task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(ctx.getOutput(), dac));
      }
    }

    if (ctx.getFunction().getType().isMutable()) {
      var dac = DataAccessContext.generate(task.getMain(), AccessLevel.ALL, verId);
      task.setAllocMainUrl(contentUrlGenerator.generateAllocateUrl(ctx.getMain(), dac));
    }
    task.setMainKeys(genUrls(ctx.getMain(), ctx.getMainRefs(),
      AccessLevel.ALL));

    var inputContextKeys = new ArrayList<String>();
    for (int i = 0; i < ctx.getInputs().size(); i++) {
      var inputObj = ctx.getInputs().get(i);
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
                                     Map<String, OaasObject> refs,
                                     AccessLevel level) {
    Map<String, String> m = new HashMap<>();
    generateUrls(m, obj, refs, "", level);
    return m;
  }

  private void generateUrls(Map<String, String> map,
                            OaasObject obj,
                            Map<String, OaasObject> refs,
                            String prefix,
                            AccessLevel level) {

    var verIds = obj.getState().getVerIds();
    if (verIds!=null && !verIds.isEmpty()) {
      for (var vidEntry : verIds.entrySet()) {
        var dac = DataAccessContext.generate(obj, level,
          vidEntry.getValue());
        var url =
          contentUrlGenerator.generateUrl(obj, dac, vidEntry.getKey());
        map.put(prefix + vidEntry.getKey(), url);
      }
    }

    if (obj.getState().getOverrideUrls()!=null) {
      obj.getState().getOverrideUrls()
        .forEachKeyValue((k, v) -> map.put(prefix + k, v));
    }
    if (refs!=null) {
      for (var entry : refs.entrySet()) {
        generateUrls(
          map,
          entry.getValue(),
          null,
          prefix + entry.getKey() + ".",
          AccessLevel.UNIDENTIFIED);
      }
    }
  }
}
