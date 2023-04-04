package org.hpcclab.oaas.invoker.ispn.store;

public interface ConnectionFactory<T> {
    T getConnection(String cacheName);
}
