package org.hpcclab.oaas.model.object;

public class OaasObjects {
  public static final OaasObject NULL = new OaasObject()
    .setId("NULL");

  public static boolean isNullObj(OaasObject object) {
    return object == NULL || object.getId().equals("NULL");
  }
}
