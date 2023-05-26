package org.hpcclab.oaas.invocation.validate;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ValidationContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;
import org.hpcclab.oaas.repository.EntityRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultInvocationValidator implements InvocationValidator{
  EntityRepository<String, OaasObject> objectRepo;
  EntityRepository<String, OaasFunction> funcRepo;
  EntityRepository<String, OaasClass> clsRepo;

  @Inject
  public DefaultInvocationValidator(EntityRepository<String, OaasObject> objectRepo,
                                    EntityRepository<String, OaasFunction> funcRepo,
                                    EntityRepository<String, OaasClass> clsRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
  }

  @Override
  public Uni<ValidationContext> validate(ObjectAccessLanguage oal) {
    var builder = ValidationContext.builder();
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
        var fb = cls.findFunction(oal.getFb());
        builder.functionBinding(fb);
        return funcRepo.getAsync(fb.getFunction());
      })
      .map(builder::function)
      .map(__ -> builder.build());
  }
}
