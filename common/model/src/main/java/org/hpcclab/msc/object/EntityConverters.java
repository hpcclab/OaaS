package org.hpcclab.msc.object;

import io.vertx.core.json.Json;
import org.hpcclab.msc.object.entity.function.OaasFunctionValidation;
import org.hpcclab.msc.object.entity.function.OaasWorkflow;
import org.hpcclab.msc.object.entity.object.OaasObjectOrigin;
import org.hpcclab.msc.object.entity.object.OaasObjectRequirement;
import org.hpcclab.msc.object.entity.state.OaasObjectState;
import org.hpcclab.msc.object.entity.task.Task;
import org.hpcclab.msc.object.entity.task.TaskConfiguration;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Map;

public class EntityConverters {

  private EntityConverters(){}

  static <T> String toJson(T value){
    if (value == null) return null;
    else return Json.encode(value);
  }

  static <T> T fromJson(String json, Class<T> cls){
    if (json == null) return null;
    else return Json.decodeValue(json, cls);
  }

  @Converter
  public static class MapConverter implements AttributeConverter<Map<String,String>,String> {
    @Override
    public String convertToDatabaseColumn(Map<String,String> attribute) {
      return toJson(attribute);
    }

    @Override
    public Map<String,String> convertToEntityAttribute(String dbData) {
      return fromJson(dbData, Map.class);
    }

  }
  @Converter
  public static class OriginConverter implements AttributeConverter<OaasObjectOrigin,String> {
    @Override
    public String convertToDatabaseColumn(OaasObjectOrigin attribute) {
      return toJson(attribute);
    }

    @Override
    public OaasObjectOrigin convertToEntityAttribute(String dbData) {
      return fromJson(dbData, OaasObjectOrigin.class);
    }
  }
  @Converter
  public static class StateConverter implements AttributeConverter<OaasObjectState,String> {
    @Override
    public String convertToDatabaseColumn(OaasObjectState attribute) {
      return toJson(attribute);
    }

    @Override
    public OaasObjectState convertToEntityAttribute(String dbData) {
      return fromJson(dbData, OaasObjectState.class);
    }
  }
  @Converter
  public static class RequirementConverter implements AttributeConverter<OaasObjectRequirement,String> {
    @Override
    public String convertToDatabaseColumn(OaasObjectRequirement attribute) {
      return toJson(attribute);
    }

    @Override
    public OaasObjectRequirement convertToEntityAttribute(String dbData) {
      return fromJson(dbData, OaasObjectRequirement.class);
    }
  }
  @Converter
  public static class TaskConfigConverter implements AttributeConverter<TaskConfiguration,String> {
    @Override
    public String convertToDatabaseColumn(TaskConfiguration attribute) {
      return toJson(attribute);
    }

    @Override
    public TaskConfiguration convertToEntityAttribute(String dbData) {
      return fromJson(dbData, TaskConfiguration.class);
    }
  }
  @Converter
  public static class TaskConverter implements AttributeConverter<Task,String> {
    @Override
    public String convertToDatabaseColumn(Task attribute) {
      return toJson(attribute);
    }

    @Override
    public Task convertToEntityAttribute(String dbData) {
      return fromJson(dbData, Task.class);
    }
  }
  @Converter
  public static class ValidationConverter implements AttributeConverter<OaasFunctionValidation,String> {
    @Override
    public String convertToDatabaseColumn(OaasFunctionValidation attribute) {
      return toJson(attribute);
    }

    @Override
    public OaasFunctionValidation convertToEntityAttribute(String dbData) {
      return fromJson(dbData, OaasFunctionValidation.class);
    }
  }
  @Converter
  public static class WorkflowConverter implements AttributeConverter<OaasWorkflow,String> {
    @Override
    public String convertToDatabaseColumn(OaasWorkflow attribute) {
      return toJson(attribute);
    }

    @Override
    public OaasWorkflow convertToEntityAttribute(String dbData) {
      return fromJson(dbData, OaasWorkflow.class);
    }
  }
}
