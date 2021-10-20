package org.hpcclab.msc.object.entity.function;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.NaturalId;
import org.hpcclab.msc.object.EntityConverters;
import org.hpcclab.msc.object.entity.BaseUuidEntity;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.task.TaskConfiguration;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class OaasFunction {
  @NotBlank
  @Id
  String name;

  @NotNull
  @Enumerated
  OaasFunction.FuncType type;

  @ManyToOne(fetch = FetchType.LAZY)
  @ToString.Exclude
  OaasClass outputCls;

  @Convert(converter = EntityConverters.ValidationConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasFunctionValidation validation;

  @Convert(converter = EntityConverters.TaskConfigConverter.class)
  @Column(columnDefinition = "jsonb")
  TaskConfiguration task;

  @Convert(converter = EntityConverters.WorkflowConverter.class)
  @Column(columnDefinition = "jsonb")
  OaasWorkflow macro;


  public enum FuncType {
    TASK,
    MACRO,
    LOGICAL
  }
}
