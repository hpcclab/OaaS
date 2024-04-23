package org.hpcclab.oaas.invoker.verticle;

import io.vertx.core.Verticle;
import org.hpcclab.oaas.model.cls.OClass;

import java.util.List;

public interface VerticleFactory<T extends Verticle> {
  List<T> createVerticles(OClass cls);
}
