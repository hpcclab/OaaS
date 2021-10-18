package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class OaasCompoundMember {
  String name;
  @ManyToOne(fetch = FetchType.LAZY)
  OaasObject object;
}
