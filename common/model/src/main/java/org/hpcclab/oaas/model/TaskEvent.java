package org.hpcclab.oaas.model;

import java.util.List;

public class TaskEvent {
  Type type;
  String id;
  List<String> nextTask;


  public enum Type {
    CREATE, EXEC, COMPLETE
  }
}
