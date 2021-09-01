package org.hpcclab.msc.object.entity.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.msc.object.entity.object.MscObject;
import org.hpcclab.msc.object.entity.object.MscObjectState;

import java.util.Map;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectValidation {
  Map<String, String> requiredLabel;
  MscObject.Type requiredType;
  String requiredStateType;
}
