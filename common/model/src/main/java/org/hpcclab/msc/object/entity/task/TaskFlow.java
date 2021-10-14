package org.hpcclab.msc.object.entity.task;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hpcclab.msc.object.EntityConverters;
import org.hpcclab.msc.object.entity.object.OaasObject;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

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
