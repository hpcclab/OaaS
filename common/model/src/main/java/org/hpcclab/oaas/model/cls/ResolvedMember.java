package org.hpcclab.oaas.model.cls;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.Copyable;
import org.hpcclab.oaas.model.Views;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.state.KeySpecification;

import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class ResolvedMember implements Copyable<ResolvedMember> {
  private Map<String, FunctionBinding> functions;
  private Map<String, KeySpecification> keySpecs;
  private Map<String, ReferenceSpecification> refSpecs;
  private Set<String> identities;
  @JsonView(Views.Internal.class)
  private boolean flag = false;

  public ResolvedMember() {
  }

  public ResolvedMember(Map<String, FunctionBinding> functionBindings,
                        Map<String, KeySpecification> keySpecs,
                        Map<String, ReferenceSpecification> refSpecs,
                        Set<String> identities,
                        boolean flag) {
    this.functions = functionBindings;
    this.keySpecs = keySpecs;
    this.refSpecs = refSpecs;
    this.identities = identities;
    this.flag = flag;
  }

  @Override
  public ResolvedMember copy() {
    return new ResolvedMember(
      functions==null ? null:Map.copyOf(functions),
      keySpecs==null ? null:Map.copyOf(keySpecs),
      refSpecs==null ? null:Map.copyOf(refSpecs),
      identities==null? null : Set.copyOf(identities),
      flag
    );
  }
}
