package org.hpcclab.oaas.invocation.validate;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ValidatedInvocationContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultInvocationValidator implements InvocationValidator{
  @Inject
  ObjectRepository objectRepo;
  @Inject
  FunctionRepository funcRepo;
  @Inject
  ClassRepository clsRepo;


  @Override
  public Uni<ValidatedInvocationContext> validate(ObjectAccessLanguage oal) {
    var builder = ValidatedInvocationContext.builder();
    builder.oal(oal);
    Uni<OaasClass> uni;
    if (oal.getTarget() != null) {
      uni = objectRepo.getAsync(oal.getTarget())
        .onItem().ifNull()
        .failWith(() -> new InvocationException("Target object does not exist"))
        .invoke(builder::main)
        .flatMap(obj -> clsRepo.getAsync(obj.getCls())
          .invoke(builder::mainCls)
        );
    } else if (oal.getTargetCls() != null){
      uni = clsRepo.getAsync(oal.getTargetCls())
        .invoke(builder::targetCls);
    } else {
      throw new InvocationException("Target and TargetCls can not be null at the same time", 400);
    }

    return uni
      .flatMap(cls -> {
        var fb = cls.findFunction(oal.getFbName());
        builder.functionBinding(fb);
        return funcRepo.getAsync(fb.getFunction());
      })
      .map(builder::function)
      .map(__ -> builder.build());
  }
}
