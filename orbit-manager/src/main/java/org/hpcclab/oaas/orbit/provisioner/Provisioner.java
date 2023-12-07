package org.hpcclab.oaas.orbit.provisioner;

import java.util.function.Consumer;

public interface Provisioner<T,R> {
  R provision(T functionRecord);
}
