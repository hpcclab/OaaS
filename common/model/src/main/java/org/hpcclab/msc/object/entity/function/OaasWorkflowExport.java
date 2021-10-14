package org.hpcclab.msc.object.entity.function;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Embeddable;
import javax.persistence.Entity;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OaasWorkflowExport {
  String from;
  String as;
}
