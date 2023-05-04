package org.hpcclab.oaas.sa.producer;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
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
    var gen = new PresignGenerator(S3ClientBuilderUtil.createPresignerBuilder(saConfig.s3()).build());
    pool.put(PresignGeneratorPool.DEFAULT, gen);

    var pubGen = new PresignGenerator(S3ClientBuilderUtil
      .createPresignerBuilder(saConfig.s3())
      .endpointOverride(saConfig.s3().publicUrl())
      .build());
    pool.putPub(PresignGeneratorPool.DEFAULT, pubGen);
    return pool;
  }
}
