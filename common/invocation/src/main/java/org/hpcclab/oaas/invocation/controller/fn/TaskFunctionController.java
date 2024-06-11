package org.hpcclab.oaas.invocation.controller.fn;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.invocation.InvocationCtx;
import org.hpcclab.oaas.invocation.controller.SimpleStateOperation;
import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.invocation.task.InvokingDetail;
import org.hpcclab.oaas.invocation.task.OffLoader;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.IOObject;
import org.hpcclab.oaas.model.object.OMeta;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.model.state.StateType;
import org.hpcclab.oaas.model.task.OTask;
import org.hpcclab.oaas.model.task.TaskCompletion;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public class TaskFunctionController extends AbstractFunctionController {
  private static final Logger logger = LoggerFactory.getLogger(TaskFunctionController.class);

  final OffLoaderFactory offLoaderFactory;
  final ContentUrlGenerator contentUrlGenerator;
  OffLoader offloader;


  public TaskFunctionController(IdGenerator idGenerator,
                                ObjectMapper mapper,
                                OffLoaderFactory offLoaderFactory,
                                ContentUrlGenerator contentUrlGenerator) {
    super(idGenerator, mapper);
    this.offLoaderFactory = offLoaderFactory;
    this.contentUrlGenerator = contentUrlGenerator;
  }

  @Override
  protected void validate(InvocationCtx ctx) {
    InvocationRequest req = ctx.getRequest();
    if (req.invId()==null || req.invId().isEmpty()) {
      req = req.toBuilder()
        .invId(idGenerator.generate())
        .build();
      ctx.setRequest(req);
    }
    if ((req.main()==null || req.main().isEmpty()) && !functionBinding.isNoMain()) {
      throw new InvocationException(
        "Function '%s' is not marked to be called without main"
          .formatted(functionBinding.getName()),
        400);
    }

  }

  @Override
  protected void afterBind() {
    this.offloader = offLoaderFactory.create(function);
  }

  @Override
  protected Uni<InvocationCtx> exec(InvocationCtx ctx) {
    ctx.setImmutable(functionBinding.isImmutable() || !function.getType().isMutable());
    if (outputCls!=null) {
      var output = createOutput(ctx);
      ctx.setOutput(output);
    }
    OTask task = genTask(ctx);
    InvokingDetail<OTask> invokingDetail = InvokingDetail.of(task, getFunction());
    var uni = offloader.offload(invokingDetail);
    return uni
      .map(tc -> handleComplete(ctx, tc));
  }

  public GOObject createOutput(InvocationCtx ctx) {
    OMeta meta = new OMeta();
    meta.setCls(outputCls.getKey());
    meta.setRevision(0);
    var req = ctx.getRequest();
    var outId = req!=null ? req.outId():null;
    if (outId!=null && !outId.isEmpty()) {
      meta.setId(outId);
    } else {
      meta.setId(idGenerator.generate());
    }
    return new GOObject(meta);
  }

  public OTask genTask(InvocationCtx ctx) {
    var verId = ctx.getRequest().invId();
    var task = new OTask();
    task.setId(verId);
    task.setPartKey(ctx.getRequest().main());
    task.setFbName(functionBinding.getName());
    task.setMain(ctx.getMain());
    task.setFuncKey(function.getKey());
    task.setImmutable(ctx.isImmutable());
    task.setReqBody(ctx.getRequest().body());
    task.setArgs(resolveArgs(ctx));

    task.setMainKeys(generateUrls(ctx.getMain(), ctx.getMainRefs(), AccessLevel.ALL));
    if (ctx.getOutput()!=null) {
      task.setOutput(ctx.getOutput());
      if (outputCls.getStateType()==StateType.COLLECTION) {
        var dac = DataAccessContext.generate(task.getOutput(), AccessLevel.ALL, verId);
        task.setAllocOutputUrl(contentUrlGenerator.generateAllocateUrl(ctx.getOutput(), dac));
      } else {
        task.setOutputKeys(generatePutUrls(ctx.getOutput(),
          outputCls, verId, AccessLevel.ALL));
      }
    }

    if (function.getType().isMutable() && task.getMain()!=null) {
      var dac = DataAccessContext.generate(task.getMain(), AccessLevel.ALL, verId);
      task.setAllocMainUrl(contentUrlGenerator.generateAllocateUrl(ctx.getMain(), dac));
    }
    task.setTs(System.currentTimeMillis());
    return task;
  }


  public Map<String, String> generateUrls(IOObject<?> obj,
                                          Map<String, ? extends IOObject> refs,
                                          AccessLevel level) {
    Map<String, String> m = new HashMap<>();
    if (obj!=null) generateUrls(m, obj, refs, "", level);
    return m;
  }

  public Map<String, String> generatePutUrls(IOObject<?> obj,
                                             OClass cls,
                                             String verId,
                                             AccessLevel level) {
    if (obj==null) return Map.of();
    List<KeySpecification> keySpecs = cls.getStateSpec().getKeySpecs();
    Map<String, String> map = new HashMap<>();
    for (KeySpecification keySpec : keySpecs) {
      var dac = DataAccessContext.generate(obj, level, verId);
      var url = contentUrlGenerator.generatePutUrl(obj, dac, keySpec.getName());
      map.put(keySpec.getName(), url);
    }
    return map;
  }

  private void generateUrls(Map<String, String> map,
                            IOObject<?> obj,
                            Map<String, ? extends IOObject> refs,
                            String prefix,
                            AccessLevel level) {
    if (obj==null) return;
    var verIds = obj.getMeta().getVerIds();
    if (verIds!=null && !verIds.isEmpty()) {
      for (var vidEntry : verIds.entrySet()) {
        var dac = DataAccessContext.generate(obj, level,
          vidEntry.getValue());
        var url = contentUrlGenerator.generateUrl(obj, dac, vidEntry.getKey());
        map.put(prefix + vidEntry.getKey(), url);
      }
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

  public Map<String, String> resolveArgs(InvocationCtx ctx) {
    var defaultArgs = functionBinding.getDefaultArgs();
    if (ctx.getArgs()!=null && defaultArgs!=null) {
      var finalArgs = Maps.mutable.ofMap(defaultArgs);
      finalArgs.putAll(ctx.getArgs());
      return finalArgs;
    } else if (ctx.getArgs()==null && defaultArgs!=null) {
      return defaultArgs;
    } else if (ctx.getArgs()!=null) {
      return ctx.getArgs();
    }
    return Map.of();
  }

  public InvocationCtx handleComplete(InvocationCtx context, TaskCompletion completion) {
    validateCompletion(context, completion);
    updateState(context, completion);
    List<GOObject> updateList = completion.getMain()!=null && !functionBinding.isImmutable() ?
      Lists.mutable.of(context.getMain()):
      List.of();
    SimpleStateOperation stateOperation;
    if (outputCls==null) {
      stateOperation = SimpleStateOperation.updateObjs(updateList, cls);
    } else {
      List<GOObject> createList = completion.getOutput()!=null ?
        List.of(context.getOutput()):
        List.of();
      stateOperation = new SimpleStateOperation(createList, outputCls, updateList, cls);
    }
    context.setStateOperations(List.of(stateOperation));
    return context;
  }

  void updateState(InvocationCtx context, TaskCompletion completion) {
    var main = context.getMain();
    var out = context.getOutput();
    var log = context.initLog();
    log.updateStatus(completion);

    if (main!=null) {
      if (completion.getMain()!=null) {
        completion.getMain().update(main, log.getKey());
      }
      main.getMeta().setLastOffset(context.getMqOffset());
    }

    if (out!=null) {
      if (completion.getOutput()!=null)
        completion.getOutput().update(out, completion
          .getId());
    }

    context.setRespBody(completion.getBody());
  }

  private void validateCompletion(InvocationCtx context, TaskCompletion completion) {
    if (completion.getMain()!=null) {
      var update = completion.getMain();
      if (!functionBinding.isImmutable()) {
        update.filterKeys(cls);
      }
    }
    if (context.getOutput()==null) {
      completion.setOutput(null);
    } else if (completion.getOutput()!=null) {
      var update = completion.getOutput();
      update.filterKeys(cls);
    }
  }
}
