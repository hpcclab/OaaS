package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface CrComponentController<T> {
  void init(CrController parentController);
//  List<T> createDeployOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec);
  List<T> createDeployOperation(CrDeploymentPlan instanceSpec);
//  List<T> createAdjustOperation(CrInstanceSpec instanceSpec, CrDataSpec dataSpec);
  List<T> createAdjustOperation(CrAdjustmentPlan instanceSpec);
  List<T> createDeleteOperation();
  void updateStableTime();
  long getStableTime();
}
