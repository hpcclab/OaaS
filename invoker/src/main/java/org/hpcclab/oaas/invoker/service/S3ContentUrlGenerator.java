package org.hpcclab.oaas.invoker.service;

import org.hpcclab.oaas.invocation.task.SaContentUrlGenerator;
import org.hpcclab.oaas.invoker.InvokerConfig;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.repository.store.DatastoreConf;
import org.hpcclab.oaas.storage.PresignGenerator;
import org.hpcclab.oaas.storage.S3ClientBuilderUtil;
import org.hpcclab.oaas.storage.S3ConnConf;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

import static org.hpcclab.oaas.storage.S3ClientBuilderUtil.createPresigner;

public class S3ContentUrlGenerator extends SaContentUrlGenerator {
  PresignGenerator presignGenerator;
  PresignGenerator pubPresignGenerator;
  String prefixPath;
  String bucket;

  public S3ContentUrlGenerator(DatastoreConf datastoreConf) {
    super(datastoreConf.options().get("URL"));
    bucket = datastoreConf.options().get("BUCKET");
    prefixPath = datastoreConf.options().get("PREFIXPATH");
    presignGenerator = new PresignGenerator(createPresigner(datastoreConf, false));
    pubPresignGenerator = new PresignGenerator(createPresigner(datastoreConf, true));
  }


  @Override
  public String generateUrl(OObject obj,
                            DataAccessContext dac,
                            String file) {
    var gen = dac.isPub()? pubPresignGenerator: presignGenerator;
    return gen.generatePresignGet(bucket,
      prefixPath + "%s/%s/%s".formatted(obj.getId(), dac.getVid(), file));
  }
}
