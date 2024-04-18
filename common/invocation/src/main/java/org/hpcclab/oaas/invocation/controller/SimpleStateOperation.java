package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.OObject;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class SimpleStateOperation implements StateOperation{
  final List<OObject> createObjs;
  final OClass createCls;
  final List<OObject> updateObjs;
  final OClass updateCls;

  public SimpleStateOperation(List<OObject> createObjs,
                              OClass createCls,
                              List<OObject> updateObjs,
                              OClass updateCls) {
    this.createObjs = createObjs;
    this.createCls = createCls;
    this.updateObjs = updateObjs;
    this.updateCls = updateCls;
  }

  public static SimpleStateOperation createObjs(List<OObject> createObjs,
                                                OClass createCls){
    return new SimpleStateOperation(createObjs, createCls, List.of(), null);
  }
  public static SimpleStateOperation createObjs(OObject createObj,
                                                OClass createCls){
    return new SimpleStateOperation(List.of(createObj), createCls, List.of(), null);
  }
  public static SimpleStateOperation updateObjs(List<OObject> updateObjs,
                                                OClass updateCls){
    return new SimpleStateOperation(List.of(), null, updateObjs, updateCls);
  }
  public static SimpleStateOperation updateObjs(OObject updateObj,
                                                OClass updateCls){
    return new SimpleStateOperation(List.of(), null, List.of(updateObj), updateCls);
  }

  public List<OObject> getCreateObjs() {
    return createObjs;
  }

  public List<OObject> getUpdateObjs() {
    return updateObjs;
  }

  public OClass getCreateCls() {
    return createCls;
  }

  public OClass getUpdateCls() {
    return updateCls;
  }
}
