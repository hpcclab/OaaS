package org.hpcclab.oaas.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DeepOaasCompoundMemberDto {
  String name;
  OaasObjectDto object;
}
