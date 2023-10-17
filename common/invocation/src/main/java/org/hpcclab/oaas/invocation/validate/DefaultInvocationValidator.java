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
    if (oal.getMain() != null) {
      uni = objectRepo.async().getAsync(oal.getMain())
        .onItem().ifNull()
        .failWith(() -> new InvocationException("Target object does not exist"))
        .invoke(builder::main)
        .flatMap(obj -> clsRepo.async().getAsync(obj.getCls())
          .invoke(builder::cls)
        );
    } else if (oal.getCls() != null){
      uni = clsRepo.async().getAsync(oal.getCls())
        .invoke(builder::cls);
    } else {
      throw new InvocationException("Target and TargetCls can not be null at the same time", 400);
    }

    return uni
      .flatMap(cls -> {
        var fb = cls.findFunction(oal.getFb());
        builder.funcBind(fb);
        return funcRepo.async().getAsync(fb.getFunction());
      })
      .map(builder::func)
      .map(__ -> builder.build());
  }
}
