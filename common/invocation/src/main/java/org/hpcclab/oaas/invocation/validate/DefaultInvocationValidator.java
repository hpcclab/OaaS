package org.hpcclab.oaas.invocation.validate;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.ValidationContext;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.exception.InvocationException;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.EntityRepository;
import org.hpcclab.oaas.repository.FunctionRepository;
import org.hpcclab.oaas.repository.ObjectRepoManager;

@ApplicationScoped
public class DefaultInvocationValidator implements InvocationValidator {
  ObjectRepoManager objectRepo;
  EntityRepository<String, OaasFunction> funcRepo;
  EntityRepository<String, OaasClass> clsRepo;

  @Inject
  public DefaultInvocationValidator(ObjectRepoManager objectRepo,
                                    FunctionRepository funcRepo,
                                    ClassRepository clsRepo) {
    this.objectRepo = objectRepo;
    this.funcRepo = funcRepo;
    this.clsRepo = clsRepo;
  }

  @Override
  public Uni<ValidationContext> validate(ObjectAccessLanguage oal) {
    var builder = ValidationContext.builder();
    builder.oal(oal);
    if (oal.getCls()==null)
      throw new InvocationException("Cls can not be null", 400);
    Uni<OaasClass> uni = clsRepo.async().getAsync(oal.getCls())
      .invoke(builder::cls);
    if (oal.getMain()!=null) {
      uni = uni.call(cls -> objectRepo.getOrCreate(cls)
        .async().getAsync(oal.getMain())
        .onItem().ifNull()
        .failWith(() -> new InvocationException("Target object does not exist"))
        .invoke(builder::main)
      );
    }

    return uni
      .flatMap(cls -> {
        var fb = cls.findFunction(oal.getFb());
        builder.fnBind(fb);
        return funcRepo.async().getAsync(fb.getFunction());
      })
      .map(builder::func)
      .map(__ -> builder.build());
  }
}
