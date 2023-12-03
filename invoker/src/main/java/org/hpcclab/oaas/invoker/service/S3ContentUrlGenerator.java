package org.hpcclab.oaas.invoker.service;

import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import org.hpcclab.oaas.storage.S3ConnConf;

public class S3ContentUrlGenerator extends SaContentUrlGenerator {
  PresignGenerator presignGenerator;
  PresignGenerator pubPresignGenerator;
  S3ConnConf s3ConnConf;
  String prefixPath;

  public S3ContentUrlGenerator(InvokerConfig config) {
    super(config.storageAdapterUrl());
    s3ConnConf = config.s3();
    presignGenerator = new PresignGenerator(S3ClientBuilderUtil.createPresigner(config.s3(), false));
    pubPresignGenerator = new PresignGenerator(S3ClientBuilderUtil.createPresigner(config.s3(), true));
    prefixPath = s3ConnConf.prefix().orElse("");
  }

  @Override
  public String generateUrl(OObject obj,
                            DataAccessContext dac,
                            String file) {
    var gen = dac.isPub()? pubPresignGenerator: presignGenerator;
    return gen.generatePresignGet(s3ConnConf.bucket(),
      prefixPath + "%s/%s/%s".formatted(obj.getId(), dac.getVid(), file));
  }
}
