package org.hpcclab.oaas.invocation.state;

import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.object.GOObject;

import java.util.List;

/**
 * @author Pawissanutt
 */
public record DeleteStateOperation(List<GOObject> objs,
                                   OClass cls) implements StateOperation{
}
