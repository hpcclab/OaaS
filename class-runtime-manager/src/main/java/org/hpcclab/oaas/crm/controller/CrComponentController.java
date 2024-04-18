package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.optimize.CrDataSpec;
import org.hpcclab.oaas.crm.optimize.CrInstanceSpec;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface CrComponentController<T> {
  void init(CrController parentController);
  List<T> createDeployOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec);
  List<T> createAdjustOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec);
  List<T> createDeleteOperation();
  void updateStabilizationTime();
}
