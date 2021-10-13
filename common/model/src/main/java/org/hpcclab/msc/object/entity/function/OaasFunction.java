package org.hpcclab.msc.object.entity.function;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hpcclab.msc.object.entity.OaasClass;
import org.hpcclab.msc.object.entity.object.OaasObjectRequirement;
import org.hpcclab.msc.object.entity.object.OaasObjectTemplate;
import org.hpcclab.msc.object.entity.task.TaskConfiguration;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class OaasFunction {
  //  @BsonId
  @NotBlank
  @Id
  String name;
  @NotNull
  OaasFunction.FuncType type;
  boolean reactive = false;

  @ManyToMany
  List<OaasClass> outputClass;
  OaasObjectRequirement bindingRequirement;

  @ElementCollection
  List<OaasObjectRequirement> additionalInputs = List.of();

  TaskConfiguration task;
//  @Type(type = "json")
//  @Column(columnDefinition = "jsonb")
//  @ElementCollection
//  Map<String, SubFunctionCall> subFunctions = Map.of();
  @ManyToOne(
    cascade = CascadeType.ALL,
    fetch = FetchType.EAGER
  )
  OaasWorkflow macro;


  public enum FuncType {
    TASK,
    MACRO,
    LOGICAL
  }
}
