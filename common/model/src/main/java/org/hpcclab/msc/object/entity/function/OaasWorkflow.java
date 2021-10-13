package org.hpcclab.msc.object.entity.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.OaasClass;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
public class OaasWorkflow {
  @Id
  @GeneratedValue
  Long id;
  @OneToMany(
    cascade = CascadeType.ALL,
    fetch = FetchType.EAGER
  )
  List<OaasWorkflowStep> steps;
  @ManyToOne
  OaasClass outputClass;
  @ElementCollection
  Set<OaasWorkflowExport> exports;
}
