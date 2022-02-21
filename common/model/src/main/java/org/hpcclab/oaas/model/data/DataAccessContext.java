package org.hpcclab.oaas.model.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.TaskContext;
import org.hpcclab.oaas.model.proto.OaasObject;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataAccessContext {
  UUID mainId;
  String mainCls;
  UUID outId;
  String outCls;
  List<UUID> inputIds;
  List<String> inputCls;

  public DataAccessContext() {
  }

  public DataAccessContext(TaskContext taskContext) {
    mainId = taskContext.getMain().getId();
    mainCls = taskContext.getMain().getCls();
    outId = taskContext.getOutput().getId();
    outCls = taskContext.getOutput().getCls();
    inputIds = taskContext.getInputs()
      .stream().map(OaasObject::getId).toList();
    inputCls = taskContext.getInputs()
      .stream()
      .map(OaasObject::getCls).toList();
  }

  public String getCls(UUID id) {
    if (mainId.equals(id)) return mainCls;
    if (outId.equals(id)) return outCls;
    var i = inputIds.indexOf(id);
    if (i < 0) return null;
    return inputCls.get(i);
  }
}
