package org.hpcclab.oaas.model.proto;

import org.eclipse.collections.api.factory.Maps;
import org.hpcclab.oaas.model.HasKey;
import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.types.java.collections.AbstractCollectionAdapter;

@ProtoAdapter(CollectionMap.class)
public class CollectionMapAdapter<V extends HasKey<String>> extends AbstractCollectionAdapter<CollectionMap<V>, V> {

    @ProtoFactory
    public CollectionMap<V> create(int size) {
        if (size > 0) {
            return CollectionMap.wrap(Maps.mutable.empty());
        } else
            return CollectionMap.of();
    }
}
