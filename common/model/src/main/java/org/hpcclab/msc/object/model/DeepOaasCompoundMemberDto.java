package org.hpcclab.msc.object.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class DeepOaasCompoundMemberDto {
  String name;
  OaasObjectDto object;
}
