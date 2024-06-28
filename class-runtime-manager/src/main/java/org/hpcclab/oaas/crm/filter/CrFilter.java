package org.hpcclab.oaas.crm.filter;

/**
 * @author Pawissanutt
 */
public interface CrFilter<V> {
  V applyOnCreate(V item);
  V applyOnAdjust(V item);
  default V applyOnDelete(V item) {
    return item;
  }
  String name();
}
