package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;

public interface VerticleFactory<T extends Verticle> {
  T createVerticle(String suffix);
  default T createVerticle(){
    return createVerticle(null);
  }
}
