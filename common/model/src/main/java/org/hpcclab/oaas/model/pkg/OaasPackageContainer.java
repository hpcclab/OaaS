package org.hpcclab.oaas.model.pkg;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class OaasPackageContainer extends OaasPackage{
  List<OClass> classes = List.of();
  List<OFunction> functions = List.of();
}
