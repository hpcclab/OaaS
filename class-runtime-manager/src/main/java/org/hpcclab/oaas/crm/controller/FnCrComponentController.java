package org.hpcclab.oaas.crm.controller;

import org.hpcclab.oaas.crm.filter.CrFilter;
import org.hpcclab.oaas.crm.optimize.CrAdjustmentPlan;
import org.hpcclab.oaas.crm.optimize.CrDeploymentPlan;
import org.hpcclab.oaas.proto.OFunctionStatusUpdate;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface FnCrComponentController<T>  extends CrComponentController<T>{
  OFunctionStatusUpdate buildStatusUpdate();

  class NoOp<T> implements FnCrComponentController<T> {

    @Override
    public void init(CrController parentController) {

    }

    @Override
    public List<T> createDeployOperation(CrDeploymentPlan instanceSpec) {
      return List.of();
    }

    @Override
    public List<T> createAdjustOperation(CrAdjustmentPlan instanceSpec) {
      return List.of();
    }

    @Override
    public List<T> createDeleteOperation() {
      return List.of();
    }

    @Override
    public void updateStableTime() {

    }

    @Override
    public long getStableTime() {
      return 0;
    }

    @Override
    public void addFilter(CrFilter<List<T>> filter) {

    }

    @Override
    public OFunctionStatusUpdate buildStatusUpdate() {
      return null;
    }
  }
}
