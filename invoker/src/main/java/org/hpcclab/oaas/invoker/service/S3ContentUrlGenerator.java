package org.hpcclab.oaas.invoker.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import org.hpcclab.oaas.storage.S3ConnConf;
import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.data.AccessLevel;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.exception.StdOaasException;
import org.hpcclab.oaas.model.object.OaasObject;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3ContentUrlGenerator extends SaContentUrlGenerator {
  S3Presigner presigner;
  PresignGenerator presignGenerator;
  S3ConnConf s3ConnConf;

  public S3ContentUrlGenerator(InvokerConfig config) {
    super(config.storageAdapterUrl());
    s3ConnConf = config.s3();
    presigner = S3ClientBuilderUtil.createPresigner(config.s3());
    presignGenerator = new PresignGenerator(presigner);
  }

  public String generateUrl(OaasObject obj,
                            String file,
                            AccessLevel level) {
    if (obj.getState().getVerIds() == null || obj.getState().getVerIds().isEmpty())
      throw StdOaasException.notKeyInObj(obj.getId(),404);
    var vid = obj.getState().findVerId(file);
    if (vid == null)
      throw StdOaasException.notKeyInObj(obj.getId(),404);
    var dac = DataAccessContext.generate(obj, level, vid);
    return generateUrl(obj, dac, file);
  }

  public String generateUrl(OaasObject obj,
                            DataAccessContext dac,
                            String file) {
    return presignGenerator.generatePresignGet(s3ConnConf.bucket(),
      s3ConnConf.prefix().orElse("") + "%s/%s/%s".formatted(obj.getId(),dac.getVid(),file));
  }
}
