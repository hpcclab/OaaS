package org.hpcclab.oprc.cli;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * @author Pawissanutt
 */
public class JsonUtil {

  public static DocumentContext parse(String json) {
    return JsonPath.parse(json, Configuration.builder()
      .mappingProvider(new JacksonMappingProvider())
      .jsonProvider(new JacksonJsonProvider())
      .options(Option.SUPPRESS_EXCEPTIONS)
      .build());
  }
}
