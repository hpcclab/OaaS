package org.hpcclab.oaas.sa.producer;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;
import org.hpcclab.oaas.sa.SaConfig;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.PresignGeneratorPool;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3ClientProducer {
  @Inject
  SaConfig saConfig;
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
