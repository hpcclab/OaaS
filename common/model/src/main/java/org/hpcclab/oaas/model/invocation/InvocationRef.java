package org.hpcclab.oaas.model.invocation;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.Objects;

public final class InvocationRef {
    private final String key;
    private final String cls;

    @ProtoFactory
    public InvocationRef(String key, String cls) {
        this.key = key;
        this.cls = cls;
    }

    @ProtoField(1)
    public String key() {
        return key;
    }

    @ProtoField(2)
    public String cls() {
        return cls;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj==this) return true;
        if (obj==null || obj.getClass()!=this.getClass()) return false;
        var that = (InvocationRef) obj;
        return Objects.equals(this.key, that.key) &&
                Objects.equals(this.cls, that.cls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, cls);
    }

    @Override
    public String toString() {
        return "InvocationRef[" +
                "key=" + key + ", " +
                "cls=" + cls + ']';
    }

}
