package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.filter.CrFilter;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface CrComponentController<T> {
  void init(CrController parentController);
  List<T> createDeployOperation(CrDeploymentPlan instanceSpec);
  List<T> createAdjustOperation(CrAdjustmentPlan instanceSpec);
  List<T> createDeleteOperation();
  void updateStableTime();
  long getStableTime();
  void addFilter(CrFilter<List<T>> filter);


}
