package org.hpcclab.oaas.entity.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OaasCompoundMember {
  String name;
  @NotNull
  @JoinColumn
  @ManyToOne(fetch = FetchType.LAZY)
  OaasObject object;
}
