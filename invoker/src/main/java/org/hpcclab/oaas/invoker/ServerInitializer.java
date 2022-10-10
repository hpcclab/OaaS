package org.hpcclab.oaas.invoker;

import io.quarkus.runtime.StartupEvent;
import org.hpcclab.oaas.arango.ArgRepositoryInitializer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class ServerInitializer {
  @Inject
  ArgRepositoryInitializer initializer;


  void onStart(@Observes StartupEvent startupEvent){
    initializer.setup();
  }

}
