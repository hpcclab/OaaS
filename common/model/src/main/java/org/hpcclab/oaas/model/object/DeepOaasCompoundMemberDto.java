package org.hpcclab.oaas.model.object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.proto.OaasObject;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DeepOaasCompoundMemberDto {
  String name;
  OaasObject object;
}
