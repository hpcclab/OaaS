package org.hpcclab.oaas.model.oal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.object.ObjectOrigin;
import org.hpcclab.oaas.model.proto.KvPair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuperBuilder
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ObjectAccessLanguage {
  // language=RegExp
  private static final String EXPR_REGEX =
    "^(?:_(?<cls>[a-zA-Z0-9._-]+[~/]?))?(?<main>[a-zA-Z0-9-]*)(?::(?<fn>[a-zA-Z0-9._-]+)(?:\\((?<inputs>[a-zA-Z0-9,-]*)\\)(\\((?<args>[^)]*)\\))?)?)?$";
  private static final Pattern EXPR_PATTERN = Pattern.compile(EXPR_REGEX);
  final String main;
  final String cls;
  final String fn;
  final Map<String, String> args;
  final List<String> inputs;

  @JsonCreator
  public ObjectAccessLanguage(String main, String mainCls, String fbName, Map<String, String> args, List<String> inputs) {
    this.main = main;
    this.cls = mainCls;
    this.fn = fbName;
    this.args = args;
    this.inputs = inputs;
  }

  public static ObjectAccessLanguage from(ObjectOrigin origin) {
    return ObjectAccessLanguage.builder()
      .main(origin.getParentId())
      .fn(origin.getFbName())
      .args(origin.getArgs().stream().collect(Collectors.toMap(KvPair::getKey, KvPair::getVal)))
      .inputs(origin.getInputs())
      .build();
  }

  public static boolean validate(String expr) {
    return EXPR_PATTERN.matcher(expr).matches();
  }

  public static ObjectAccessLanguage parse(String expr) {
    var matcher = EXPR_PATTERN.matcher(expr);
    if (!matcher.find())
      throw new OalParsingException("The given expression('" + expr + "') doesn't match the pattern.");
    var main = matcher.group("main");
    var cls = matcher.group("cls");
    var fn = matcher.group("fn");
    var inputs = matcher.group("inputs");
    var args = matcher.group("args");
    var oal = ObjectAccessLanguage.builder();
    if (!(main==null || main.isEmpty())) {
      oal.main = main;
    }
    if (!(cls==null || cls.isEmpty())) {
      oal.cls = cls;
    }
    if (fn==null) return oal.build();
    oal.fn = fn;
    if (inputs!=null && !inputs.isEmpty()) {
      var list = Arrays.stream(inputs.split(","))
        .toList();
      oal.inputs(list);
    }
    if (args!=null && !args.isEmpty()) {
      var argMap = Arrays.stream(args.split(","))
        .map(pair -> {
          var kv = pair.split("=");
          if (kv.length!=2) throw new OalParsingException("Arguments parsing exception");
          return Map.entry(kv[0], kv[1]);
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      oal.args(argMap);
    }
    return oal.build();
  }

  public InvocationRequest.InvocationRequestBuilder toRequest() {
    return InvocationRequest.builder()
      .main(main)
      .cls(cls)
      .fb(fn)
      .args(args)
      .inputs(inputs);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (cls==null || cls.isEmpty()) {
      sb.append(main);
    } else {
      sb.append('_')
        .append(cls);

      if (!(main==null || main.isEmpty()))
        sb.append("~")
          .append(main);
    }

    if (fn==null)
      return sb.toString();
    sb.append(':').append(fn).append('(');
    var size = inputs==null ? 0:inputs.size();
    for (int i = 0; i < size; i++) {
      if (i!=0) sb.append(',');
      sb.append(inputs.get(i));
    }
    sb.append(')');


    if (args!=null && !args.isEmpty()) {
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
}
