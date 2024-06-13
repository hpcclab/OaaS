package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.GOObject;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class SimpleStateOperation implements StateOperation {
  final List<GOObject> createObjs;
  final OClass createCls;
  final List<GOObject> updateObjs;
  final OClass updateCls;

  public SimpleStateOperation(List<GOObject> createObjs,
                              OClass createCls,
                              List<GOObject> updateObjs,
                              OClass updateCls) {
    this.createObjs = createObjs;
    this.createCls = createCls;
    this.updateObjs = updateObjs;
    this.updateCls = updateCls;
  }

  public static SimpleStateOperation createObjs(List<GOObject> createObjs,
                                                OClass createCls) {
    return new SimpleStateOperation(createObjs, createCls, List.of(), null);
  }

  public static SimpleStateOperation createObjs(GOObject createObj,
                                                OClass createCls) {
    return new SimpleStateOperation(List.of(createObj), createCls, List.of(), null);
  }

  public static SimpleStateOperation updateObjs(List<GOObject> updateObjs,
                                                OClass updateCls) {
    return new SimpleStateOperation(List.of(), null, updateObjs, updateCls);
  }

  public static SimpleStateOperation updateObjs(GOObject updateObj,
                                                OClass updateCls) {
    return new SimpleStateOperation(List.of(), null, List.of(updateObj), updateCls);
  }

  public List<GOObject> getCreateObjs() {
    return createObjs;
  }

  public List<GOObject> getUpdateObjs() {
    return updateObjs;
  }

  public OClass getCreateCls() {
    return createCls;
  }

  public OClass getUpdateCls() {
    return updateCls;
  }
}
