package org.hpcclab.oaas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.mapper.ProtoObjectMapperImpl;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.proto.OaasSchemaImpl;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.proto.ProtoOObject;
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

  OObject object;
  ObjectMapper mapper;
  ObjectMapper msgpackMapper;
  ProtoOObject protoOObject;
  Random random;
  byte[] jsonBytes;
  byte[] msgpackBytes;
  byte[] protostreamBytes;
  byte[] protoByte;

  ProtoObjectMapper protoMapper ;

  SerializationContext context;

  public static void main(String[] args) throws RunnerException {

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
  }

  @Setup(Level.Trial)
  public void doSetup() throws IOException {
    mapper = new ObjectMapper();
    msgpackMapper = new MessagePackMapper();
    protoMapper = new ProtoObjectMapperImpl();
    protoMapper.setMapper(msgpackMapper);
    random = new Random();
    var refs = IntStream.range(0, 5)
      .mapToObj(__ -> Map.entry(rand(), rand()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var node = genNode();
    object = new OObject()
      .setId(rand())
      .setCls(rand())
      .setRefs(DSMap.copy(refs))
      .setState(new OaasObjectState()
        .setVerIds(DSMap.of(rand(), rand(), rand(), rand(), rand(), rand()))
      )
      .setData(node);
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
  public OObject testDeserializeJson() throws IOException {
    return mapper.readValue(jsonBytes, OObject.class);
  }

  @Benchmark
  public OObject testDeserializeMsgpack() throws IOException {
    return msgpackMapper.readValue(msgpackBytes, OObject.class);
  }

  @Benchmark
  public OObject testDeserializeProtoStream() throws IOException {
    return ProtobufUtil.fromByteArray(context, protostreamBytes, OObject.class);
  }
  @Benchmark
  public ProtoOObject testDeserializeProto() throws IOException {
    return ProtoOObject.parseFrom(protoByte);
  }

  @Benchmark
  public OObject testDeserializeProto2() throws IOException {
    return protoMapper.fromProto(ProtoOObject.parseFrom(protoByte));
  }

  @Benchmark
  public OObject fromProto() {
    return protoMapper.fromProto(protoOObject);
  }
  @Benchmark
  public ProtoOObject toProto() {
    return protoMapper.toProto(object);
  }

  @TearDown(Level.Trial)
  public void doTearDown() {
    // tear down code here
  }
}
