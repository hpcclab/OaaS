package org.hpcclab.oaas.proto;

import org.infinispan.protostream.annotations.ProtoAdapter;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import java.util.HashMap;
import java.util.List;

@ProtoAdapter(HashMap.class)
public class MapAdapter {
    @ProtoFactory
    public HashMap<String, String> create(List<Entry> entries) {
        var map = new HashMap<String, String>();
        for (Entry entry : entries) {
            map.put(entry.key, entry.value);
        }
        return map;
    }

    @ProtoField(1)
    public List<Entry> getEntries(HashMap<String,String> map) {
        return map.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .toList();
    }

    public static class Entry {
        String key;
        String value;

        @ProtoFactory
        public Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }
        @ProtoField(1)
        public String getKey() {
            return key;
        }

        @ProtoField(2)
        public String getValue() {
            return value;
        }
    }
}
