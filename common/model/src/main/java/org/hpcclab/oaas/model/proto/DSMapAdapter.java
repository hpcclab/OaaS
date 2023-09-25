package org.hpcclab.oaas.model.proto;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoAdapter(DSMap.class)
public class DSMapAdapter {
    @ProtoFactory
    public DSMap create(String[] keys, String[] values) {
        var map = Lists.fixedSize.of(keys)
                .zip(Lists.immutable.of(values))
                .toMap(Pair::getOne, Pair::getTwo);
        return DSMap.wrap(map);
    }

    @ProtoField(1)
    public String[] getKeys(DSMap map) {
        String[] keys = new String[map.size()];
        return map.keySet().toArray(keys);
    }

    @ProtoField(2)
    public String[] getValues(DSMap map) {
        String[] values = new String[map.size()];
        return map.values().toArray(values);
    }

}
