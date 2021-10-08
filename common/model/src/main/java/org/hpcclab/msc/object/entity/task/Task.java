package org.hpcclab.msc.object.entity.task;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.function.MscFunction;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.state.MscObjectState;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
  String id;
  MscObject main;
  MscObject output;
  MscFunction function;
  String requestFile;
  Map<String, String> args= Map.of();
  List<MscObject> additionalInputs = List.of();

  public static String createId(MscObject outputObj, String requestFile) {
    if (outputObj.getState().getType() == MscObjectState.Type.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
}
