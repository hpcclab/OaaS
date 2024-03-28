package org.hpcclab.oaas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.json.JsonObject;
import org.hpcclab.oaas.model.OprcJsonUtil;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BenchmarkMode({Mode.Throughput})
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class MapSerializationBenchmark {

  Map<String, Object> map;
  JsonObject jsonObject;
  ObjectNode nodes;
  ObjectMapper mapper;
  ObjectMapper msgpackMapper;
  Random random;
  byte[] jsonBytes;
  byte[] msgpackBytes;

  public static void main(String[] args) throws RunnerException {

    Options opt = new OptionsBuilder()
      .include(MapSerializationBenchmark.class.getSimpleName())
      .forks(1)
      .warmupIterations(2)
      .measurementIterations(3)
      .warmupTime(TimeValue.seconds(5))
      .measurementTime(TimeValue.seconds(5))
      .build();

    new Runner(opt).run();
  }

  @Setup(Level.Trial)
  public void doSetup() throws IOException {
    mapper = new ObjectMapper();
    mapper.registerModule(OprcJsonUtil.createModule());
    msgpackMapper = new MessagePackMapper();
    random = new Random();
    var node = genNode();
    jsonBytes = mapper.writeValueAsBytes(node);
    msgpackBytes = msgpackMapper.writeValueAsBytes(node);
    System.out.println("\njson length: " + jsonBytes.length);
    System.out.println("msgpack length: " + msgpackBytes.length);
    map = mapper.readValue(jsonBytes, Map.class);
    jsonObject = new JsonObject(map);
//    System.out.println(jsonObject.encode());
  }



  String rand() {
    Random random = new Random();
    return IntStream.range(0, 10)
      .mapToObj(i -> String.valueOf((char) random.nextInt('a', 'z')))
      .collect(Collectors.joining());
  }

  ObjectNode genNode() {
    var node = mapper.createObjectNode();
    for (int i = 0; i < 5; i++) {
      node.put(rand(), rand());
    }
    for (int i = 0; i < 5; i++) {
      node.put(rand(), random.nextInt());
    }
    for (int i = 0; i < 5; i++) {
      node.put(rand(), random.nextBoolean());
    }
    for (int i = 0; i < 5; i++) {
      node.put(rand(), random.nextFloat());
    }
    return node;
  }

  @Benchmark
  public byte[] testSerializeJsonMap() throws JsonProcessingException {
    return mapper.writeValueAsBytes(map);
  }
  @Benchmark
  public byte[] testSerializeJsonNode() throws JsonProcessingException {
    return mapper.writeValueAsBytes(nodes);
  }

  @Benchmark
  public byte[] testSerializeJsonObject() throws JsonProcessingException {
    return mapper.writeValueAsBytes(jsonObject);
  }

//  @Benchmark
//  public byte[] testSerializeMsgpack() throws JsonProcessingException {
//    return msgpackMapper.writeValueAsBytes(object);
//  }

  @Benchmark
  public ObjectNode testDeserializeJsonNode() throws IOException {
    return mapper.readValue(jsonBytes, ObjectNode.class);
  }

  @Benchmark
  public Map<String, Object> testDeserializeJsonMap() throws IOException {
    return mapper.readValue(jsonBytes, Map.class);
  }
  @Benchmark
  public JsonObject testDeserializeJsonObject() throws IOException {
    return mapper.readValue(jsonBytes, JsonObject.class);
  }
//
//  @Benchmark
//  public OObject testDeserializeMsgpack() throws IOException {
//    return msgpackMapper.readValue(msgpackBytes, OObject.class);
//  }

  @TearDown(Level.Trial)
  public void doTearDown() {
    // tear down code here
  }
}
