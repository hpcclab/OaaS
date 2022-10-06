package org.hpcclab.oaas.model.cls;

import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.state.KeySpecification;

import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ResolvedMember implements Copyable<ResolvedMember> {
  Map<String, FunctionBinding> functions;
  Map<String, KeySpecification> keySpecs;
  Map<String, ReferenceSpecification> refSpecs;
  Set<String> identities;
  boolean fFinal = false;

  public ResolvedMember() {
  }

  public ResolvedMember(Map<String, FunctionBinding> functionBindings,
                        Map<String, KeySpecification> keySpecs,
                        Map<String, ReferenceSpecification> refSpecs,
                        Set<String> identities) {
    this(
      functionBindings,
      keySpecs,
      refSpecs,
      identities,
      false
    );
  }

  public ResolvedMember(Map<String, FunctionBinding> functionBindings,
                        Map<String, KeySpecification> keySpecs,
                        Map<String, ReferenceSpecification> refSpecs,
                        Set<String> identities,
                        boolean fFinal) {
    this.functions = functionBindings;
    this.keySpecs = keySpecs;
    this.refSpecs = refSpecs;
    this.identities = identities;
    this.fFinal = fFinal;
  }

  @Override
  public ResolvedMember copy() {
    return new ResolvedMember(
      functions==null ? null:Map.copyOf(functions),
      keySpecs==null ? null:Map.copyOf(keySpecs),
      refSpecs==null ? null:Map.copyOf(refSpecs),
      identities==null? null : Set.copyOf(identities),
      fFinal
    );
  }
}
