package org.hpcclab.oaas.invoker.ispn;

import org.hpcclab.oaas.invoker.ispn.store.ValueMapper;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JOObject;
import org.hpcclab.oaas.model.object.JsonBytes;

/**
 * @author Pawissanutt
 */
public class GJValueMapper  implements ValueMapper<GOObject, JOObject> {
  @Override
  public JOObject mapToDb(GOObject goObject) {
    return new JOObject(goObject.getMeta(), goObject.getData().getNode());
  }

  @Override
  public GOObject mapToCStore(JOObject joObject) {
    return new GOObject(joObject.getMeta(), new JsonBytes(joObject.getData()));
  }
}
