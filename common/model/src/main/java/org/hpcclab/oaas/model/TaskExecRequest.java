package org.hpcclab.oaas.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.entity.object.OaasObjectOrigin;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class TaskExecRequest {
  String id;
  List<Map<String,OaasObjectOrigin>> originList;
}
