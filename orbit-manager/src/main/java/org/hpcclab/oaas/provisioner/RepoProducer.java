package org.hpcclab.oaas.provisioner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.arango.AutoRepoBuilder;
import org.hpcclab.oaas.arango.repo.ArgClsRepository;
import org.hpcclab.oaas.arango.repo.ArgFunctionRepository;
import org.hpcclab.oaas.arango.repo.ArgObjectRepository;
import org.hpcclab.oaas.repository.ClassRepository;
import org.hpcclab.oaas.repository.ClassResolver;

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

//  @Produces
//  @ApplicationScoped
//  ArgObjectRepository objRepository() {
//    return AutoRepoBuilder.objRepository();
//  }

  @Produces
  @ApplicationScoped
  ClassResolver classResolver(ClassRepository classRepository) {
    return new ClassResolver(classRepository);
  }
}
