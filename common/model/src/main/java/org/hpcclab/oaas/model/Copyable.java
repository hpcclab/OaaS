package org.hpcclab.oaas.model;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;

import java.util.List;
import java.util.Set;

public interface Copyable <T>{
  T copy();

 static <T> List<T> copy(List<Copyable<T>> original) {
    return Lists.fixedSize.ofAll(original)
      .collect(o -> o.copy());
  }
 static <T> Set<T> copy(Set<Copyable<T>> original) {
    return Sets.fixedSize.ofAll(original)
      .collect(o -> o.copy());
  }
}
