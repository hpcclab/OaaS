package org.hpcclab.oaas.entity;

import io.vertx.core.json.Json;
import org.hpcclab.oaas.model.function.OaasFunctionValidation;
import org.hpcclab.oaas.model.function.OaasWorkflow;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;
import org.hpcclab.oaas.model.object.OaasObjectRequirement;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.model.task.OaasTask;
import org.hpcclab.oaas.model.task.TaskConfiguration;

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
//  @Converter
//  public static class RequirementConverter implements AttributeConverter<OaasObjectRequirement,String> {
//    @Override
//    public String convertToDatabaseColumn(OaasObjectRequirement attribute) {
//      return toJson(attribute);
//    }
//
//    @Override
//    public OaasObjectRequirement convertToEntityAttribute(String dbData) {
//      return fromJson(dbData, OaasObjectRequirement.class);
//    }
//  }

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
//  @Converter
//  public static class TaskConverter implements AttributeConverter<OaasTask,String> {
//    @Override
//    public String convertToDatabaseColumn(OaasTask attribute) {
//      return toJson(attribute);
//    }
//
//    @Override
//    public OaasTask convertToEntityAttribute(String dbData) {
//      return fromJson(dbData, OaasTask.class);
//    }
//  }
//
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
