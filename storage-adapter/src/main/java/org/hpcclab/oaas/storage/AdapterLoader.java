package org.hpcclab.oaas.storage;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.eclipse.collections.api.factory.Lists;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.state.KeySpecification;
import org.hpcclab.oaas.storage.adapter.InternalDataAllocateRequest;
import org.hpcclab.oaas.storage.adapter.S3Adapter;
import org.hpcclab.oaas.storage.adapter.StorageAdapter;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;

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
    // NOTE: We currently only have S3 implementation.
    return s3Adapter;
  }

  public  Uni<Map<String, String>> aggregatedAllocate(DataAllocateRequest request) {
    var requests= Lists.fixedSize.ofAll(request.getKeys())
      .groupBy(ks -> Objects.requireNonNullElse(ks.getProvider(), request.getDefaultProvider()))
      .keyMultiValuePairsView()
      .collect(entry -> new InternalDataAllocateRequest(
        request.getOid(), entry.getTwo().collect(KeySpecification::getName).toList(), entry.getOne(), request.isPublicUrl()))
      .toList();

    return Multi.createFrom().iterable(requests)
      .onItem().transformToUniAndConcatenate(this::aggregatedAllocate)
      .collect().asList()
      .map(l -> l.stream().reduce(new HashMap<>(), (m1,m2) -> {
        m1.putAll(m2);
        return m1;
      }));
  }

  public Uni<Map<String,String>> aggregatedAllocate(InternalDataAllocateRequest request) {
    if (Objects.equals(request.getProvider(), s3Adapter.name())) {
      return s3Adapter.allocate(request);
    } else {
      return Uni.createFrom().nullItem();
    }
  }
}
