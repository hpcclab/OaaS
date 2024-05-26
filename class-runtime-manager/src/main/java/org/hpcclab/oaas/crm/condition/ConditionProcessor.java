package org.hpcclab.oaas.crm.condition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.hpcclab.oaas.crm.condition.ConditionOperation.IS_NULL;

@Singleton
public class ConditionProcessor {
  private static final Logger logger = LoggerFactory.getLogger( ConditionProcessor.class );

  Configuration conf;
  ObjectMapper mapper;

  public ConditionProcessor(ObjectMapper mapper) {
    this.mapper = mapper;
    conf = Configuration
      .builder()
      .mappingProvider(new JacksonMappingProvider(mapper))
      .jsonProvider(new JacksonJsonProvider(mapper))
      .build();
  }

  public boolean matches(Condition condition,
                         Object data) {
    if (condition == null) return true;
    try {
      var ctx = JsonPath.parse(mapper.writeValueAsString(data));
      boolean bool = allMatches(condition.all(), ctx);
      bool &= anyMatches(condition.any(), ctx);
      bool &= singleMatches(condition, ctx);
      return bool;
    } catch (JsonProcessingException e) {
      logger.warn("json parsing error", e);
      return false;
    }
  }

  public boolean matches(Condition condition,
                         String data) {
    var ctx = JsonPath.parse(data);
    boolean bool = allMatches(condition.all(), ctx);
    bool &= anyMatches(condition.any(), ctx);
    bool &= singleMatches(condition, ctx);
    return bool;
  }

  public boolean allMatches(Set<Condition> conditions,
                            DocumentContext node) {
    if (conditions==null || conditions.isEmpty()) return true;
    for (Condition condition : conditions) {
      if (!singleMatches(condition, node)) {
        return false;
      }
    }
    return true;
  }

  public boolean anyMatches(Set<Condition> conditions,
                            DocumentContext node) {
    if (conditions==null || conditions.isEmpty()) return true;
    for (Condition condition : conditions) {
      if (singleMatches(condition, node)) {
        return true;
      }
    }
    return false;
  }

  public boolean singleMatches(Condition condition,
                               DocumentContext node) {
    if (condition.path()==null) return true;
    try {
      Object target = node.read(condition.path());
      return switch (target) {
        case null -> condition.op()==IS_NULL;
        case Number number -> condition.op().check(number, condition.val());
        case String string -> condition.op().check(string, condition.val());
        case Boolean bool -> condition.op().check(bool, condition.val());
        default -> false;
      };
    } catch (PathNotFoundException e) {
      return condition.op()==IS_NULL;
    }
  }
}
