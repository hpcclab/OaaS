package org.hpcclab.oaas.taskmanager.factory;

import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TaskEventFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(TaskEventFactory.class);

  @Inject
  OaasObjectRepository objectRepo;

  public TaskEvent createStartingEvent(String id) {
    var object = objectRepo.get(id);
    if (object==null) throw NoStackException.notFoundObject400(id);
    var origin = object.getOrigin();
    return new TaskEvent()
      .setId(id)
      .setType(TaskEvent.Type.CREATE)
      .setExec(true)
      .setEntry(true)
      .generatePrq(origin);
  }

  public List<TaskEvent> createPrqEvent(TaskEvent event,
                                        TaskEvent.Type type,
                                        Collection<String> roots) {
    if (event.getPrqTasks()==null || event.getPrqTasks().isEmpty())
      return List.of();
    var map = objectRepo.list(event.getPrqTasks());
    map.values()
      .stream()
      .filter(object -> object.getOrigin().getParentId()==null)
      .forEach(object -> roots.add(object.getId().toString()));
    return map
      .values()
      .stream()
      .filter(object -> object.getOrigin().getParentId()!=null)
      .map(obj -> new TaskEvent()
        .setId(obj.getId())
        .generatePrq(obj.getOrigin())
        .setNextTasks(Set.of(event.getId()))
        .setSource(event.getId())
        .setType(type)
      )
      .toList();
  }
}
