package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.proto.ProtoOFunction;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface FnCrControllerFactory<T> extends Filterable<List<T>> {
  FnCrComponentController<T> create(ProtoOFunction function);
}
