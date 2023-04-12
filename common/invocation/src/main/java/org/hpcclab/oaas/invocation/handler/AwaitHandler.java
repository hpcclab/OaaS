package org.hpcclab.oaas.invocation.handler;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationExecutor;
import org.hpcclab.oaas.model.invocation.InvApplyingContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.ObjectRepository;
import org.hpcclab.oaas.repository.event.ObjectCompletionListener;

public class AwaitHandler {
  final ObjectRepository objectRepo;
  final InvocationExecutor invocationExecutor;
  final InvocationHandlerService invocationHandlerService;
  final ObjectCompletionListener completionListener;

  @Inject
  public AwaitHandler(ObjectRepository objectRepo, InvocationExecutor invocationExecutor, InvocationHandlerService invocationHandlerService, ObjectCompletionListener completionListener) {
    this.objectRepo = objectRepo;
    this.invocationExecutor = invocationExecutor;
    this.invocationHandlerService = invocationHandlerService;
    this.completionListener = completionListener;
  }

  public Uni<OaasObject> awaitCompletion(OaasObject obj,
                                         Integer timeout) {
    var status = obj.getStatus();
    var ts = status.getTaskStatus();
    if (!ts.isSubmitted() && !status.isInitWaitFor()) {
      var uni1 = completionListener.wait(obj.getId(), timeout);
      var uni2 = invocationExecutor.asyncSubmit(obj);
      return Uni.combine().all().unis(uni1, uni2)
        .asTuple()
        .flatMap(v -> objectRepo.getAsync(obj.getId()));

    }
    return completionListener.wait(obj.getId(), timeout)
      .flatMap(event -> objectRepo.getAsync(obj.getId()));
  }

  public Uni<InvApplyingContext> asyncInvoke(ObjectAccessLanguage oal,
                                             boolean await,
                                             int timeout) {
    Uni<InvApplyingContext> uni = invocationHandlerService.applyFunction(oal);
    return uni
      .flatMap(ctx -> {
        if (completionListener.enabled() && await && ctx.getOutput()!=null) {
          var id = ctx.getOutput().getId();
          var uni1 = completionListener.wait(id, timeout);
          var uni2 = invocationExecutor.asyncSubmit(ctx);
          return Uni.combine().all().unis(uni1, uni2)
            .asTuple()
            .replaceWith(ctx);
        }
        return invocationExecutor.asyncSubmit(ctx)
          .replaceWith(ctx);
      });
  }
}
