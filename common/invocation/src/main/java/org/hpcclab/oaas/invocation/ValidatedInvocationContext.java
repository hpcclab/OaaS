package org.hpcclab.oaas.invocation;

import lombok.Builder;
import lombok.Getter;
import org.hpcclab.oaas.model.cls.OaasClass;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OaasFunction;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;
import org.hpcclab.oaas.model.object.OaasObject;

@Builder()
public record ValidatedInvocationContext(
    ObjectAccessLanguage oal,
    OaasObject main,
    OaasClass mainCls,
    OaasClass targetCls,
    OaasFunction function,
    FunctionBinding functionBinding
  ) {
}
