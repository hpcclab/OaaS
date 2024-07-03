package org.hpcclab.oaas.crm.filter;

/**
 * @author Pawissanutt
 */
public interface CrFilter<V> {
  default V applyOnCreate(V item) {
    return item;
  }
  default V applyOnAdjust(V item) {
    return item;
  }
  default V applyOnDelete(V item) {
    return item;
  }
  String name();
}
