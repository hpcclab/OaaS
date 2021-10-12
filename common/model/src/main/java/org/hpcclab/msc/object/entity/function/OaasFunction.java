package org.hpcclab.msc.object.entity.function;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import lombok.experimental.Accessors;
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
//@TypeDef(name = "json", typeClass = JsonType.class)
public class OaasFunction {
  //  @BsonId
  @NotBlank
  @Id
  String name;
  @NotNull
  OaasFunction.FuncType type;
  boolean reactive = false;
//  @Type(type = "json")
//  @Column(columnDefinition = "jsonb")
//  OaasObjectTemplate outputTemplate;
  @ManyToMany
  List<OaasClass> outputClass;
  OaasObjectRequirement bindingRequirement;
//  @Type(type = "json")
//  @Column(columnDefinition = "jsonb")
  @ElementCollection
  List<OaasObjectRequirement> additionalInputs = List.of();
//  @Type(type = "json")
//  @Column(columnDefinition = "jsonb")
  TaskConfiguration task;
//  @Type(type = "json")
//  @Column(columnDefinition = "jsonb")
//  @ElementCollection
//  Map<String, SubFunctionCall> subFunctions = Map.of();
  OaasWorkflow macro;


  public enum FuncType {
    TASK,
    MACRO,
    LOGICAL
  }
}
