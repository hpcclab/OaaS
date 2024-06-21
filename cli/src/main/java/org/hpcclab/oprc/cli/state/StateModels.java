package org.hpcclab.oprc.cli.state;

import lombok.Builder;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;

import java.util.List;

/**
 * @author Pawissanutt
 */
public interface StateModels {
  @Builder(toBuilder = true)
  record LocalPackage (
    List<OClass> classes,
    List<OFunction> functions
  ){}
}
