package org.hpcclab.oaas.storage;

import io.smallrye.mutiny.Multi;
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
import java.util.stream.Collectors;

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
    var requests = cls.getStateSpec().getKeySpecs()
      .stream()
      .collect(Collectors.groupingBy(KeySpecification::getProvider))
      .entrySet()
      .stream()
      .map(entry -> new DataAllocateRequest()
        .setOid(oid)
        .setProvider(entry.getKey())
        .setPublicUrl(isPublic)
        .setKeys(entry.getValue().stream().map(KeySpecification::getName).toList())
      )
      .toList();
    return Multi.createFrom().iterable(requests)
      .onItem().transformToUniAndConcatenate(this::aggregatedAllocate)
      .collect().asList()
      .map(l -> l.stream().reduce(new HashMap<>(), (m1,m2) -> {
        m1.putAll(m2);
        return m1;
      }));
  }


  public Uni<Map<String,String>> aggregatedAllocate(DataAllocateRequest request) {
    if (request.getProvider().equals(s3Adapter.name())) {
      return s3Adapter.allocate(request);
    } else {
      return Uni.createFrom().nullItem();
    }
  }
}
