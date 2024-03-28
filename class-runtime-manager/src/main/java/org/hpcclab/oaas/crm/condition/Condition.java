package org.hpcclab.oaas.crm.condition;

import lombok.Builder;

import java.util.Set;

@Builder(toBuilder = true)
public record Condition(Set<Condition> all,
                        Set<Condition> any,
                        String path,
                        ConditionOperation op,
                        String val
  )
  {}
