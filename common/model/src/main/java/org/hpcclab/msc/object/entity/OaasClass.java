package org.hpcclab.msc.object.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.NaturalId;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasClass {
  @Id
  String name;
  OaasObject.ObjectType objectType;
  OaasObjectState.StateType stateType;
  @ElementCollection(fetch = FetchType.LAZY)
  List<OaasFunctionBinding> functions;
}
