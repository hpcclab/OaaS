package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

@Data
@Accessors(chain = true)
public class OaasCompoundMemberDto {
  String name;
  OaasObjectDto object;
}
