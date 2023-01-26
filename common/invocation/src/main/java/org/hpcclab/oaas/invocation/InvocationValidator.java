package org.hpcclab.oaas.invocation;

import io.smallrye.mutiny.Uni;
import org.hpcclab.oaas.model.oal.ObjectAccessLanguage;

public interface InvocationValidator {
  Uni<ValidatedInvocationContext> validate(ObjectAccessLanguage oal);

}
