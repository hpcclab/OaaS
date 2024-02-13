package org.hpcclab.oaas.sa.cdi;

import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.PresignGeneratorPool;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;

public class S3ClientProducer {
  @Produces
  public PresignGeneratorPool presignGenerator() {
    var pool =  new PresignGeneratorPool();
    var conf = DatastoreConfRegistry.getDefault()
      .getOrDefault("S3DEFAULT");
    var gen = new PresignGenerator(S3ClientBuilderUtil.createPresigner(conf, false));
    pool.put(PresignGeneratorPool.DEFAULT, gen);
    var pubGen = new PresignGenerator(S3ClientBuilderUtil.createPresigner(conf, true));
    pool.put(PresignGeneratorPool.DEFAULT, gen);
    pool.putPub(PresignGeneratorPool.DEFAULT, pubGen);
    return pool;
  }
}
