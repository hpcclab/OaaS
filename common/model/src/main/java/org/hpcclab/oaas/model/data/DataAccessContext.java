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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DataAccessContext {
  String mainId;
  String mainCls;
  String outId;
  String outCls;
  List<String> inputIds;
  List<String> inputCls;
  String sig;

  public DataAccessContext() {
  }

  public DataAccessContext(TaskContext taskContext) {
    mainId = taskContext.getMain().getId();
    mainCls = taskContext.getMain().getCls();
    outId = taskContext.getOutput().getId();
    outCls = taskContext.getOutput().getCls();
    if (!taskContext.getInputs().isEmpty()) {
      inputIds = taskContext.getInputs()
        .stream().map(OaasObject::getId).toList();
      inputCls = taskContext.getInputs()
        .stream()
        .map(OaasObject::getCls).toList();
    }
  }

  public String getCls(String id) {
    if (mainId != null && mainId.equals(id)) return mainCls;
    if (outId != null && outId.equals(id)) return outCls;
    if (inputIds == null) return null;
    var i = inputIds.indexOf(id);
    if (i < 0) return null;
    return inputCls.get(i);
  }
}
