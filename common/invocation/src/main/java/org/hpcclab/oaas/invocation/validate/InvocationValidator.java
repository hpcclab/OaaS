package org.hpcclab.oaas.invocation.validate;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.invocation.ValidationContext;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

public interface InvocationValidator {
  Uni<ValidationContext> validate(ObjectAccessLanguage oal);

}
