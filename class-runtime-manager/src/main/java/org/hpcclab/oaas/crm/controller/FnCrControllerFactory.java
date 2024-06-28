package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.filter.CrFilter;
import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface FnCrControllerFactory<T> {
  FnCrComponentController<T> create(ProtoOFunction function);
  void addFilter(CrFilter<List<T>> filter);
}
