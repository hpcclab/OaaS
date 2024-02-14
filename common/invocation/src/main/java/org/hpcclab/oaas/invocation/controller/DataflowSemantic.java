package org.hpcclab.oaas.invocation.controller;

import org.hpcclab.oaas.model.function.DataflowStep;
import org.hpcclab.oaas.model.function.MacroSpec;

import java.util.List;

/**
 * @author Pawissanutt
 */
public class DataflowSemantic {
  MacroSpec macroSpec;

  static class DataflowNode{
    DataflowStep step;
    List<DataflowNode> require;
    List<DataflowNode> next;

  }
}
