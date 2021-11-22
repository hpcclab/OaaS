package org.hpcclab.oaas.model.provision;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnativeProvision {
  private String image;
  private int minScale = 0;
  private int maxScale = -1;
  private int concurrency = -1;
  private String requestsCpu;
  private String requestsMemory;
  private String limitsCpu;
  private String limitsMemory;
  private Map<String, String> env;
}
