package org.hpcclab.oaas.invoker.ispn.store;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Pawissanutt
 */
public enum ArgAttribute {
  UNKNOWN(null),
  ARG_STORE("arg-store"),
  VALUE_CLASS("value-class"),
  STORE_CLASS("store-class"),
  VALUE_MAPPER("value-mapper"),
  AUTO_CREATE("auto-create"),
  DATASTORE_CONF_NAME("store-name");
  private final String name;

  ArgAttribute(final String name) {
    this.name = name;
  }

  public String getLocalName() {
    return name;
  }

  private static final Map<String, ArgAttribute> MAP;

  static {
    final Map<String, ArgAttribute> map = HashMap.newHashMap(values().length);
    for (ArgAttribute element : values()) {
      final String name = element.getLocalName();
      map.put(name, element);
    }
    MAP = map;
  }

  public static ArgAttribute forName(final String localName) {
    final ArgAttribute element = MAP.get(localName);
    return element == null ? UNKNOWN : element;
  }

  @Override
  public String toString() {
    return name;
  }
}
