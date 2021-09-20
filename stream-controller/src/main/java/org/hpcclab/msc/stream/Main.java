package org.hpcclab.msc.stream;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

import javax.inject.Inject;

@QuarkusMain
public class Main implements QuarkusApplication {
  @Inject StreamCreator creator;

  @Override
  public int run(String... args) throws Exception {
    creator.create();
    return 0;
  }
}
