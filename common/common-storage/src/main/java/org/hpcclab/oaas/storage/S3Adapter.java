package org.hpcclab.oaas.storage;

import org.hpcclab.oaas.model.data.DataAccessRequest;
import org.hpcclab.oaas.model.data.DataAllocateRequest;
import org.hpcclab.oaas.repository.store.DatastoreConfRegistry;

import java.util.HashMap;
import java.util.Map;

public class S3Adapter implements StorageAdapter {
  private final PresignGeneratorPool generatorPool;
  private final String prefix;
  private final String bkt;

  public S3Adapter(PresignGeneratorPool generatorPool) {
    this.generatorPool = generatorPool;
    var datastoreConf = DatastoreConfRegistry.getDefault()
      .getOrDefault("S3DEFAULT");
    bkt = datastoreConf.options().get("BUCKET");
    prefix = datastoreConf.options().get("PREFIXPATH");
  }


  @Override
  public String get(DataAccessRequest dar) {
    var generator = generatorPool.getGenerator();
    return generator.generatePresignGet(
      bkt,
      convertToPath(dar.oid(), dar.vid(), dar.key())
    );
  }


  @Override
  public Map<String, String> allocate(DataAllocateRequest request) {
    var keys = request.getKeys();
    var map = new HashMap<String, String>();
    for (var key : keys) {
      var path = generatePath(request, key);
      var gen = request.isPublicUrl() ?
        generatorPool.getPublicGenerator():
        generatorPool.getGenerator();
      var url = gen.generatePresignPut(bkt, path);
      map.put(key, url);
    }
    return map;
  }

  String generatePath(DataAllocateRequest request, String key) {
    return convertToPath(request.getOid(), request.getVid(), key);
  }

  String convertToPath(String oid, String vid, String key) {
    return prefix +
      oid +
      '/' +
      vid +
      '/' +
      key;
  }
}
