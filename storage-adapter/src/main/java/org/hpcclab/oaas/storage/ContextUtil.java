package org.hpcclab.oaas.storage;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.data.DataAccessContext;

import java.util.Base64;

public class ContextUtil {
  private ContextUtil() {
  }

  public static DataAccessContext parseDac(String contextKey) {
    if (contextKey==null) return null;
    var dacJson = Base64.getUrlDecoder().decode(contextKey);
    return Json.decodeValue(new String(dacJson), DataAccessContext.class);
  }
}
