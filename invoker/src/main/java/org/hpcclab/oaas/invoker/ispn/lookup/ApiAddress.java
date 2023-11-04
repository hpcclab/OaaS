package org.hpcclab.oaas.invoker.ispn.lookup;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.io.Serializable;


public record ApiAddress( String host,  int port) {
    @ProtoFactory
    public ApiAddress {
    }

    @Override
    @ProtoField(1)
    public String host() {
        return host;
    }

    @Override
    @ProtoField(value = 2, defaultValue = "-1")
    public int port() {
        return port;
    }

}
