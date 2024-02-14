package org.hpcclab.oaas.invocation;

import lombok.Builder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OObject;

@Builder()
public record ValidationContext(
    ObjectAccessLanguage oal,
    OObject main,
    OClass cls,
    OFunction func,
    FunctionBinding fnBind
  ) {
}
