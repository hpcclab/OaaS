package org.hpcclab.oaas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.mapper.ProtoMapper;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.object.GOObject;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.proto.OaasSchemaImpl;
import org.hpcclab.oaas.proto.ProtoPOObject;
import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.SerializationContext;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class SerializationBenchmark {

  GOObject object;
  ObjectMapper mapper;
  ObjectMapper msgpackMapper;
  ProtoPOObject protoOObject;
  Random random;
  byte[] jsonBytes;
  byte[] msgpackBytes;
  byte[] protostreamBytes;
  byte[] protoByte;

  ProtoMapper protoMapper;

  SerializationContext context;

  public static void main(String[] args) throws RunnerException, IOException {

    Options opt = new OptionsBuilder()
      .mode(Mode.AverageTime)
      .timeUnit(TimeUnit.MICROSECONDS)
      .include(SerializationBenchmark.class.getSimpleName())
      .forks(1)
      .warmupIterations(2)
      .measurementIterations(3)
      .warmupTime(TimeValue.seconds(5))
      .measurementTime(TimeValue.seconds(5))
      .build();

    new Runner(opt).run();

//    SerializationBenchmark benchmark = new SerializationBenchmark();
//    benchmark.doSetup();
  }

  @Setup(Level.Trial)
  public void doSetup() throws IOException {
    mapper = new ObjectMapper();
    msgpackMapper = new MessagePackMapper();
    protoMapper = new ProtoMapperImpl();
    random = new Random();
    var refs = IntStream.range(0, 5)
      .mapToObj(__ -> Map.entry(rand(), rand()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var node = genNode();
    object = new GOObject();
    object.getMeta()
      .setId(rand())
      .setCls(rand())
      .setRefs(DSMap.copy(refs))
      .setVerIds(DSMap.of(rand(), rand(), rand(), rand(), rand(), rand()));
    object.setData(new JsonBytes(node));
    object
      .setRevision(random.nextLong());
    protoOObject = protoMapper.toProto(object);
    context = ProtobufUtil.newSerializationContext();
    var schema = new OaasSchemaImpl();
    schema.registerSchema(context);
    schema.registerMarshallers(context);

    jsonBytes = mapper.writeValueAsBytes(object);
    msgpackBytes = msgpackMapper.writeValueAsBytes(object);
    protostreamBytes = ProtobufUtil.toByteArray(context, object);
    protoByte = protoOObject.toByteArray();
    System.out.println("\njson length: " + jsonBytes.length);
    System.out.println("msgpack length: " + msgpackBytes.length);
    System.out.println("protostream length: " + protostreamBytes.length);
    System.out.println("proto length: " + protoByte.length);
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
  public byte[] testSerializeJson() throws JsonProcessingException {
    return mapper.writeValueAsBytes(object);
  }

  @Benchmark
  public byte[] testSerializeMsgpack() throws JsonProcessingException {
    return msgpackMapper.writeValueAsBytes(object);
  }

  @Benchmark
  public byte[] testSerializeProtoStream() throws IOException {
    return ProtobufUtil.toByteArray(context, object);
  }

  @Benchmark
  public byte[] testSerializeProto() throws IOException {
    return protoOObject.toByteArray();
  }

  @Benchmark
  public byte[] testSerializeProto2() throws IOException {
    return protoMapper.toProto(object).toByteArray();
  }


  @Benchmark
  public GOObject testDeserializeJson() throws IOException {
    return mapper.readValue(jsonBytes, GOObject.class);
  }

  @Benchmark
  public GOObject testDeserializeMsgpack() throws IOException {
    return msgpackMapper.readValue(msgpackBytes, GOObject.class);
  }

  @Benchmark
  public GOObject testDeserializeProtoStream() throws IOException {
    return ProtobufUtil.fromByteArray(context, protostreamBytes, GOObject.class);
  }

  @Benchmark
  public ProtoPOObject testDeserializeProto() throws IOException {
    return ProtoPOObject.parseFrom(protoByte);
  }

  @Benchmark
  public GOObject testDeserializeProto2() throws IOException {
    return protoMapper.fromProto(ProtoPOObject.parseFrom(protoByte));
  }

  @Benchmark
  public GOObject fromProto() {
    return protoMapper.fromProto(protoOObject);
  }

  @Benchmark
  public ProtoPOObject toProto() {
    return protoMapper.toProto(object);
  }

  @TearDown(Level.Trial)
  public void doTearDown() {
    // tear down code here
  }
}
