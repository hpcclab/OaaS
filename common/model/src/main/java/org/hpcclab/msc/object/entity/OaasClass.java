package org.hpcclab.msc.object.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.NaturalId;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClass extends BaseUuidEntity{
  @NaturalId
  String name;
  OaasObjectState.StateType stateType;
  @ElementCollection
  Set<OaasFunctionBinding> functions;
}
