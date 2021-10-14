package org.hpcclab.msc.object.entity.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.Hibernate;
import org.hpcclab.msc.object.entity.BaseUuidEntity;
import org.hpcclab.msc.object.entity.OaasClass;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasWorkflow {
  List<OaasWorkflowStep> steps;
  Set<OaasWorkflowExport> exports;
}
