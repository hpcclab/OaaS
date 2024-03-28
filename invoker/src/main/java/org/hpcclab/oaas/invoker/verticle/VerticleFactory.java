package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import org.hpcclab.oaas.model.cls.OClass;

public interface VerticleFactory<T extends Verticle> {
  T createVerticle(OClass cls);
  default T createVerticle(){
    return createVerticle(null);
  }
}
