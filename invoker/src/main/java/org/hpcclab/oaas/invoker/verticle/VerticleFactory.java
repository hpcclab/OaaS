package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import org.hpcclab.oaas.model.cls.OaasClass;

public interface VerticleFactory<T extends Verticle> {
  T createVerticle(OaasClass cls);
  default T createVerticle(){
    return createVerticle(null);
  }
}
