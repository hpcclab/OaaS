package org.hpcclab.msc.object.entity.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.List;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class OaasWorkflowStep {
  @Id
  @GeneratedValue
  Long id;
  String funcName;
  String target;
  String as;
  @ElementCollection
  List<String> inputRefs;
}
