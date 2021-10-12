package org.hpcclab.msc.object.entity.function;

import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.persistence.Embeddable;

@Embeddable
public class OaasFunctionBinding {
  AccessModifier access;
  String functionName;

  public enum AccessModifier {
    PUBLIC,
    INTERNAL,
    PRIVATE
  }
}
