package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.invocation.task.ContentUrlGenerator;
import org.hpcclab.oaas.model.data.DataAccessContext;
import org.hpcclab.oaas.model.object.IOObject;
import org.hpcclab.oaas.repository.store.DatastoreConf;

import static org.hpcclab.oaas.storage.S3ClientBuilderUtil.createPresigner;

public class UnifyContentUrlGenerator implements ContentUrlGenerator {
  PresignGenerator presignGenerator;
  PresignGenerator pubPresignGenerator;
  String prefixAllocateUrl;
  String prefixPath;
  String bucket;

  public UnifyContentUrlGenerator(String prefixAllocateUrl,
                                  DatastoreConf datastoreConf) {
    this.prefixAllocateUrl = prefixAllocateUrl;
    bucket = datastoreConf.options().get("BUCKET");
    prefixPath = datastoreConf.options().getOrDefault("PREFIXPATH", "");
    presignGenerator = new PresignGenerator(createPresigner(datastoreConf, false));
    pubPresignGenerator = new PresignGenerator(createPresigner(datastoreConf, true));
  }


  @Override
  public String generateUrl(IOObject<?> obj,
                            DataAccessContext dac,
                            String file) {
    var gen = dac.isPub() ? pubPresignGenerator:presignGenerator;
    return gen.generatePresignGet(bucket,
      prefixPath + "%s/%s/%s".formatted(obj.getKey(), dac.getVid(), file));
  }

  @Override
  public String generatePutUrl(IOObject<?> obj, DataAccessContext dac, String file) {
    var gen = dac.isPub() ? pubPresignGenerator:presignGenerator;
    return gen.generatePresignPut(bucket,
      prefixPath + "%s/%s/%s".formatted(obj.getKey(), dac.getVid(), file));
  }

  public String generateAllocateUrl(IOObject<?> obj, DataAccessContext dac) {
    return prefixAllocateUrl + "/allocate/%s?contextKey=%s"
      .formatted(obj.getKey(), dac.encode());
  }
}
