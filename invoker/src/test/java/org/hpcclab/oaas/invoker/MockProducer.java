package org.hpcclab.oaas.invoker;

import io.quarkus.test.Mock;
import org.hpcclab.oaas.invocation.SyncInvoker;
import org.hpcclab.oaas.test.MockSyncInvoker;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;


@ApplicationScoped
public class MockProducer {

  @Produces
  @Mock
  SyncInvoker invoker() {
    return new MockSyncInvoker();
  }
}
