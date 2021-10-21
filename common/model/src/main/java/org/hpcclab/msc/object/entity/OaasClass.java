package org.hpcclab.msc.object.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;
import org.hpcclab.msc.object.entity.function.OaasFunctionBinding;
import org.hpcclab.msc.object.entity.object.OaasObject;
import org.hpcclab.msc.object.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)

@NamedEntityGraph(
  name = "oaas.class.deep",
  attributeNodes = {
    @NamedAttributeNode(value = "functions", subgraph = "oaas.functionBinding.deep"),
  },
  subgraphs = {
    @NamedSubgraph(
      name = "oaas.functionBinding.deep",
      attributeNodes = @NamedAttributeNode(value = "function",subgraph = "oaas.function.deep")),
    @NamedSubgraph(name = "oaas.function.deep",
      attributeNodes = @NamedAttributeNode(value = "outputCls")
    )
  })

public class OaasClass {
  @Id
  String name;
  OaasObject.ObjectType objectType;
  OaasObjectState.StateType stateType;
  @ElementCollection()
  List<OaasFunctionBinding> functions;
}
