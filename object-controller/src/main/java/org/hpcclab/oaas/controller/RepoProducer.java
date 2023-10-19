package org.hpcclab.oaas.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.hpcclab.oaas.arango.AutoRepoBuilder;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.ArgObjectRepository;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;
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
  ArgObjectRepository objRepository() {
    return AutoRepoBuilder.objRepository();
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
}
