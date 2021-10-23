package org.hpcclab.oaas.entity.task;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.EntityConverters;
import org.hpcclab.oaas.entity.object.OaasObject;

import javax.persistence.*;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskFlow {
  @Id
  String id;
  @JsonIgnore
  @OneToOne
  OaasObject output;
  @Convert(converter = EntityConverters.TaskConverter.class)
  @Column(columnDefinition = "jsonb")
  Task task;
  Boolean submitted = false;

}
