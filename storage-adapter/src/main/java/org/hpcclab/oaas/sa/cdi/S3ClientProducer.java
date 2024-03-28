package org.hpcclab.oaas.sa.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.hpcclab.oaas.storage.S3Adapter;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.PresignGeneratorPool;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;

import java.util.Map;


public class S3ClientProducer {
  @Produces
  @ApplicationScoped
  public PresignGeneratorPool presignGenerator() {
    var pool =  new PresignGeneratorPool();
    DatastoreConfRegistry registry = DatastoreConfRegistry.getDefault();
    for (Map.Entry<String, DatastoreConf> entry : registry.getConfMap().entrySet()) {
      if (entry.getKey().startsWith("S3")) {
        var conf = entry.getValue();
        var gen = new PresignGenerator(S3ClientBuilderUtil.createPresigner(conf, false));
        pool.put(entry.getKey(), gen);
        var pubGen = new PresignGenerator(S3ClientBuilderUtil.createPresigner(conf, true));
        pool.put(entry.getKey(), gen);
        pool.putPub(entry.getKey(), pubGen);
      }
    }
    return pool;
  }

  @Produces
  @ApplicationScoped
  public S3Adapter s3Adapter(PresignGeneratorPool pool) {
    return new S3Adapter(pool);
  }
}
