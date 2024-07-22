package org.hpcclab.oaas.invocation.state;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.GOObject;

import java.util.List;

/**
 * @author Pawissanutt
 */
public record SimpleStateOperation(List<GOObject> createObjs,
                                   OClass createCls,
                                   List<GOObject> updateObjs,
                                   OClass updateCls) implements StateOperation {

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
}
