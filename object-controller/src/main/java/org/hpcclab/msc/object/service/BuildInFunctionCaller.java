package org.hpcclab.msc.object.service;

import org.hpcclab.msc.object.entity.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;

import javax.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class BuildInFunctionCaller {
  public MscObject call(MscObject mscObject,
                        MscFunction function,
                        Map<String, String> args) {
    if (function.getName().equals("buildin.logical.copy")) {
      return mscObject.copy().setId(null);
    } else {
      return null;
    }
  }
}
