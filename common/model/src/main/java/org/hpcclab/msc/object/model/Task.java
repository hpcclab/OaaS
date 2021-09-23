package org.hpcclab.msc.object.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.state.MscObjectState;

import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
  String id;
  String mainObj;
  String outputObj;
  String functionName;
  String resourceType;
  String image;
  List<String> commands;
  List<String> containerArgs;
  Map<String, String> env = Map.of();

  public static String createId(MscObject outputObj, String requestFile) {
    if (outputObj.getState().getType() == MscObjectState.Type.SEGMENTABLE )
      return outputObj.getId().toString() + "/" + requestFile;
    else
      return outputObj.getId().toString();
  }
}
