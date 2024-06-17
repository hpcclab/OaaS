package org.hpcclab.oaas.invoker;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.task.OffLoaderFactory;
import org.hpcclab.oaas.test.MockOffLoader;


@ApplicationScoped
public class MockProducer {

  @Mock
  @Produces
  OffLoaderFactory offLoaderFactory() {
    return new MockOffLoader.Factory();
  }
}
