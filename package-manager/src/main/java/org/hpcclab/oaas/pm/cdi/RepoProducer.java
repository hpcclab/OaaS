package org.hpcclab.oaas.pm.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.arango.AutoRepoBuilder;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.invocation.service.VertxPackageRoutes;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.repository.*;
import org.hpcclab.oaas.repository.id.IdGenerator;
import org.hpcclab.oaas.repository.id.TsidGenerator;

@ApplicationScoped
public class RepoProducer {
  @Produces
  @ApplicationScoped
  ArgClsRepository clsRepository() {
    return AutoRepoBuilder.clsRepository();
  }

  @Produces
  @ApplicationScoped
  ArgFunctionRepository funcRepository() {
    return AutoRepoBuilder.funcRepository();
  }

  @Produces
  @ApplicationScoped
  ClassResolver classResolver(ClassRepository classRepository) {
    return new ClassResolver(classRepository);
  }

  @Produces
  @Singleton
  IdGenerator idGenerator() {
    return new TsidGenerator();
  }

  @Produces
  @ApplicationScoped
  PackageValidator packageValidator(FunctionRepository functionRepository) {
    return new PackageValidator(functionRepository);
  }

  @Produces
  @ApplicationScoped
  VertxPackageRoutes vertxPackageService(ClassRepository classRepo,
                                         FunctionRepository funcRepo,
                                         PackageValidator validator,
                                         ClassResolver classResolver,
                                         ProtoMapper protoMapper,
                                         PackageDeployer packageDeployer) {
    return new VertxPackageRoutes(
      classRepo,
      funcRepo,
      validator,
      classResolver,
      protoMapper,
      packageDeployer
    );
  }
}
