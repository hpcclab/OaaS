package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.object.POObject;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class SimpleStateOperation implements StateOperation{
  final List<POObject> createObjs;
  final OClass createCls;
  final List<POObject> updateObjs;
  final OClass updateCls;

  public SimpleStateOperation(List<POObject> createObjs,
                              OClass createCls,
                              List<POObject> updateObjs,
                              OClass updateCls) {
    this.createObjs = createObjs;
    this.createCls = createCls;
    this.updateObjs = updateObjs;
    this.updateCls = updateCls;
  }

  public static SimpleStateOperation createObjs(List<POObject> createObjs,
                                                OClass createCls){
    return new SimpleStateOperation(createObjs, createCls, List.of(), null);
  }
  public static SimpleStateOperation createObjs(POObject createObj,
                                                OClass createCls){
    return new SimpleStateOperation(List.of(createObj), createCls, List.of(), null);
  }
  public static SimpleStateOperation updateObjs(List<POObject> updateObjs,
                                                OClass updateCls){
    return new SimpleStateOperation(List.of(), null, updateObjs, updateCls);
  }
  public static SimpleStateOperation updateObjs(POObject updateObj,
                                                OClass updateCls){
    return new SimpleStateOperation(List.of(), null, List.of(updateObj), updateCls);
  }

  public List<POObject> getCreateObjs() {
    return createObjs;
  }

  public List<POObject> getUpdateObjs() {
    return updateObjs;
  }

  public OClass getCreateCls() {
    return createCls;
  }

  public OClass getUpdateCls() {
    return updateCls;
  }
}
