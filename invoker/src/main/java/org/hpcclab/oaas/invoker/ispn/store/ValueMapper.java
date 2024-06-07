package org.hpcclab.oaas.invoker.ispn.store;

/**
 * @author Pawissanutt
 */
public interface ValueMapper <C,D> {
  D mapToDb(C c);
  C mapToCStore(D d);
}
