package org.hpcclab.oaas.model.oal;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;
import org.hpcclab.oaas.model.object.OaasObjectOrigin;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectAccessLangauge {
  UUID target;
  String functionName;
  Map<String, String> args;
  List<UUID> inputs;

  public static ObjectAccessLangauge from(OaasObjectOrigin origin) {
    return new ObjectAccessLangauge()
      .setTarget(origin.getParentId())
      .setFunctionName(origin.getFuncName())
      .setArgs(origin.getArgs())
      .setInputs(origin.getInputs());
  }

  public String toString() {
    StringBuffer sb = new StringBuffer(target.toString());
    if (functionName == null)
      return sb.toString();
    sb.append(':');
    sb.append(functionName);
    if (inputs== null || inputs.isEmpty()) {
      sb.append("()");
    } else {
      sb.append('(');
      for (int i = 0; i < inputs.size(); i++) {
        if (i != 0) sb.append(',');
        sb.append(inputs.get(i).toString());
      }
      sb.append(')');
    }

    if (args != null && !args.isEmpty()) {
      sb.append('(');
      var sorted = new TreeMap<>(args);
      boolean first = true;
      for (Map.Entry<String, String> entry : sorted.entrySet()) {
        if (first) first = false;
        else sb.append(',');
        sb.append(entry.getKey());
        sb.append('=');
        sb.append(entry.getValue());
      }
      sb.append(')');
    }
    return sb.toString();
  }

  // language=RegExp
  private static final String EXPR_REGEX =
    "^(?<target>[a-zA-Z0-9-]+)(:(?<func>[a-zA-Z0-9._-]+)\\((?<inputs>[a-zA-Z0-9,-]*)\\))?(\\((?<args>[^\\)]*)\\))?$";
  private static final Pattern EXPR_PATTERN = Pattern.compile(EXPR_REGEX);

  public static boolean validate(String expr) {
    return EXPR_PATTERN.matcher(expr).matches();
  }

  public static ObjectAccessLangauge parse(String expr) {
    var matcher=EXPR_PATTERN.matcher(expr);
    if (!matcher.find())
      throw new OalParsingException("The given expression('"+expr+"') doesn't match the pattern.");
    var target = matcher.group("target");
    var func = matcher.group("func");
    var inputs = matcher.group("inputs");
    var args = matcher.group("args");
    var functionCall = new ObjectAccessLangauge();
    functionCall.target = UUID.fromString(target);
    if (func == null) return functionCall;
    functionCall.functionName = func;
    if (inputs != null && !inputs.isEmpty()) {
      var list = Arrays.stream(inputs.split(","))
        .map(UUID::fromString)
        .toList();
      functionCall.setInputs(list);
    }
    if (args != null && !args.isEmpty()) {
      var argMap  = Arrays.stream(args.split(","))
        .map(pair -> {
          var kv = pair.split("=");
          if (kv.length != 2) throw new RuntimeException("Arguments parsing exception");
          return Map.entry(kv[0], kv[1]);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      functionCall.setArgs(argMap);
    }
    return functionCall;
  }
}
