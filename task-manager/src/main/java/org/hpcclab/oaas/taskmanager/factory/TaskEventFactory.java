package org.hpcclab.oaas.taskmanager.factory;

import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.exception.NoStackException;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskEvent;
import org.hpcclab.oaas.model.task.V2TaskEvent;
import org.hpcclab.oaas.repository.OaasObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TaskEventFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger( TaskEventFactory.class );

  @Inject
  OaasObjectRepository objectRepo;

  public List<TaskEvent> createTaskEventFromOriginList(List<Map<String, OaasObjectOrigin>> originList,
                                                       int traverse,
                                                       boolean exec,
                                                       String subTaskId,
                                                       TaskEvent.Type type) {
    List<TaskEvent> results = new ArrayList<>();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("createTaskEventFromOriginList originList={}", Json.encodePrettily(originList));
    }
    for (int i = 0; i < originList.size(); i++) {
      var map = originList.get(i);
      Map<String, OaasObjectOrigin> nextMap = i > 0 ? originList.get(i - 1):Map.of();
      Map<String, OaasObjectOrigin> prevMap = i + 1 < originList.size() ? originList.get(i + 1):Map.of();
      for (Map.Entry<String, OaasObjectOrigin> entry : map.entrySet()) {
        var origin = entry.getValue();
        var id = UUID.fromString(entry.getKey());

        if (origin.getParentId()==null) {
          continue;
        }

        Set<String> prevTasks = Stream.concat(
            Stream.of(origin.getParentId()),
            origin.getAdditionalInputs().stream()
          )
          .map(uuid -> OaasTask.createId(uuid.toString(), subTaskId))
          .collect(Collectors.toSet());

        Set<String> nextTasks = Set.of();

        if (!nextMap.isEmpty()) {
          nextTasks = nextMap.entrySet().stream()
            .filter(e -> e.getValue().getParentId().equals(id) ||
              e.getValue().getAdditionalInputs().contains(id)
            )
            .map(Map.Entry::getKey)
            .map(s -> OaasTask.createId(s, subTaskId))
            .collect(Collectors.toSet());
        }

        var roots = prevMap.entrySet().stream()
          .filter(e -> e.getValue().getParentId()==null)
          .filter(e -> e.getKey().equals(origin.getParentId().toString())
            || origin.getAdditionalInputs().contains(UUID.fromString(e.getKey())))
          .map(Map.Entry::getKey)
          .map(s -> OaasTask.createId(s, subTaskId))
          .collect(Collectors.toSet());

        var newEvent = new TaskEvent()
          .setType(type)
          .setId(OaasTask.createId(entry.getKey(), subTaskId))
          .setNextTasks(nextTasks)
          .setPrevTasks(prevTasks)
          .setRoots(roots)
          .setExec(exec)
          .setTraverse(i==originList.size() - 1 ? traverse:0);

        results.add(newEvent);
      }
    }
    return results;
  }

  public V2TaskEvent createStartingEvent(String id) {
    var uuid = UUID.fromString(id);
    var object = objectRepo.get(uuid);
    if (object == null) throw NoStackException.notFoundObject400(uuid);
    var origin = object.getOrigin();
    return new V2TaskEvent()
      .setId(id)
      .setType(V2TaskEvent.Type.CREATE)
      .setExec(true)
      .generatePrq(origin);
  }
  public List<V2TaskEvent> createPrqEvent(V2TaskEvent event,
                                          V2TaskEvent.Type type,
                                          Collection<String> roots) {
    if (event.getPrqTasks() == null || event.getPrqTasks().isEmpty())
      return List.of();
    var ids = event.getPrqTasks().stream()
      .map(UUID::fromString)
      .collect(Collectors.toSet());
    var map = objectRepo.list(ids);
    map.values()
      .stream()
      .filter(object -> object.getOrigin().getParentId() == null)
      .forEach(object -> roots.add(object.getId().toString()));
    return map
      .values()
      .stream()
      .filter(object -> object.getOrigin().getParentId() != null)
      .map(obj -> new V2TaskEvent()
        .setId(obj.getId().toString())
        .generatePrq(obj.getOrigin())
        .setNextTasks(Set.of(event.getId()))
        .setSource(event.getId())
        .setType(type)
      )
      .toList();
  }
}
