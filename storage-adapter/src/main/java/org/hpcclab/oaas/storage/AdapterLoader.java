package org.hpcclab.oaas.storage;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.proto.OaasClass;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.storage.adapter.S3Adapter;
import org.hpcclab.oaas.storage.adapter.StorageAdapter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AdapterLoader {
  Map<String, StorageAdapter> adapterMap = Map.of();
  @Inject
  S3Adapter s3Adapter;

  @PostConstruct
  void setup() {
    adapterMap = Map.of("s3", s3Adapter);
  }
  public StorageAdapter load(String key, OaasClass cls) {
    return s3Adapter;
  }

  public Uni<Map<String, String>> aggregatedAllocate(String oid, OaasClass cls,
                                                    boolean isPublic) {
    var dar= new DataAllocateRequest();
    dar.setOid(oid);
    Map<String, List<String>> keys = new HashMap<>();
    for (KeySpecification keySpec : cls.getStateSpec().getKeySpecs()) {
      keys.computeIfAbsent(keySpec.getProvider(), k -> new ArrayList<>())
        .add(keySpec.getName());
    }
    dar.setKeys(keys);
    dar.setPublicUrl(isPublic);
    return s3Adapter.allocate(dar);
  }

}
