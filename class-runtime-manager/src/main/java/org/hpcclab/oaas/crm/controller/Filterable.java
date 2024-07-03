package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.filter.CrFilter;

/**
 * @author Pawissanutt
 */
public interface Filterable<T> {
  void addFilter(CrFilter<T> filter);
}
