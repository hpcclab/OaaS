package org.hpcclab.oaas.invocation;

import lombok.Builder;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

@Builder()
public record ValidationContext(
    ObjectAccessLanguage oal,
    OaasObject main,
    OaasClass cls,
    OaasFunction func,
    FunctionBinding fnBind
  ) {
}
