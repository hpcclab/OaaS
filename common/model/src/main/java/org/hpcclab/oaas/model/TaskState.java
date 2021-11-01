package org.hpcclab.oaas.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Accessors(chain = true)
public class TaskState {
  Set<String> nextTasks;
  Set<String> prevTasks;
  Set<String> completedPrevTasks;
  boolean complete = false;
  boolean submitted = false;
}
