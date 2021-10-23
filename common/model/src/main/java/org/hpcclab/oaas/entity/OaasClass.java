package org.hpcclab.oaas.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.function.OaasFunctionBinding;
import org.hpcclab.oaas.entity.object.OaasObject;
import org.hpcclab.oaas.entity.state.OaasObjectState;

import javax.persistence.*;
import java.util.List;

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
      attributeNodes = @NamedAttributeNode(value = "function"
//        ,
//        subgraph = "oaas.function.deep"
      ))
//    ,
//    @NamedSubgraph(name = "oaas.function.deep",
//      attributeNodes = @NamedAttributeNode(value = "outputCls")
//    )
  })

public class OaasClass {
  @Id
  String name;
  OaasObject.ObjectType objectType;
  OaasObjectState.StateType stateType;
  @ElementCollection()
  List<OaasFunctionBinding> functions;
}
