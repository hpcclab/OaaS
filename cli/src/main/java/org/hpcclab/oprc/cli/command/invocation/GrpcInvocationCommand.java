package org.hpcclab.oprc.cli.command.invocation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.protobuf.ByteString;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientChannel;
import jakarta.inject.Inject;
import org.hpcclab.oaas.mapper.ProtoObjectMapper;
import org.hpcclab.oaas.mapper.ProtoObjectMapperImpl;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.invocation.InvocationStats;
import org.hpcclab.oaas.model.object.OObject;
import org.hpcclab.oaas.model.proto.DSMap;
import org.hpcclab.oaas.model.state.OaasObjectState;
import org.hpcclab.oaas.proto.InvocationServiceGrpc;
import org.hpcclab.oaas.proto.ProtoInvocationRequest;
import org.hpcclab.oaas.proto.ProtoInvocationResponse;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "grpc-invoke",
  aliases = {"ginv", "gi"},
  description = "Invoke a function with gRPC",
  mixinStandardHelpOptions = true
)
@RegisterForReflection(
  targets = {
    InvocationResponse.class,
    OObject.class,
    OaasObjectState.class,
    DSMap.class,
    InvocationStats.class
  }
)
public class GrpcInvocationCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(GrpcInvocationCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;

  @CommandLine.Option(names = "-c")
  String cls;

  @CommandLine.Option(names = {"-m", "--main"})
  String main;
  @CommandLine.Parameters(index = "0", defaultValue = "")
  String fb;
  @CommandLine.Option(names = "--args")
  Map<String, String> args;
  @CommandLine.Option(names = {"-i", "--inputs"})
  List<String> inputs;
  @CommandLine.Option(names = {"-b", "--pipe-body"}, defaultValue = "false")
  boolean pipeBody;
  @CommandLine.Option(names = {"-s", "--save"}, description = "save the object id to config file")
  boolean save;
  @Inject
  OutputFormatter outputFormatter;
  @Inject
  ConfigFileManager fileManager;
  @Inject
  GrpcClient grpcClient;
  @Inject
  ObjectMapper objectMapper;

  @CommandLine.Option(names = {"-a", "--async"}, defaultValue = "false")
  boolean async;

  @Override
  public Integer call() throws Exception {
    MessagePackMapper msgPackMapper = new MessagePackMapper();
    ProtoObjectMapper protoMapper = new ProtoObjectMapperImpl();
    protoMapper.setMapper(msgPackMapper);
    var conf = fileManager.current();
    var uri = URI.create(conf.getInvUrl()).toURL();
    if (cls==null) cls = conf.getDefaultClass();
    if (main==null)
      main = conf.getDefaultObject()==null ? "":conf.getDefaultObject();

    GrpcClientChannel channel = new GrpcClientChannel(grpcClient, SocketAddress.inetSocketAddress(
      uri.getPort() < 0 ? uri.getDefaultPort():uri.getPort(), uri.getHost()));
    InvocationServiceGrpc.InvocationServiceBlockingStub service =
      InvocationServiceGrpc.newBlockingStub(channel);

    var oalBuilder = ProtoInvocationRequest.newBuilder()
      .setCls(cls)
      .setMain(main)
      .setFb(fb);
    if (args!=null)
      oalBuilder.putAllArgs(args);
    if (pipeBody) {
      var body = System.in.readAllBytes();
      var sbody = new String(body).stripTrailing();
      ObjectNode objectNode = objectMapper.readValue(sbody, ObjectNode.class);
      oalBuilder.setBody(ByteString.copyFrom(msgPackMapper.writeValueAsBytes(objectNode)));
    }
    if (inputs!=null) {
      oalBuilder.addAllInputs(inputs);
    }
    var protoReq = oalBuilder.build();
    ProtoInvocationResponse response = service.invoke(protoReq);
    outputFormatter.printObject(commonOutputMixin.getOutputFormat(), protoMapper.fromProto(response));
    if (save && !response.getOutput().getId().isEmpty()) {
      String id = response.getOutput().getId();
      if (!id.isEmpty()) {
        conf.setDefaultObject(id);
        conf.setDefaultClass(response.getOutput().getCls());
        fileManager.update(conf);
      }
    }
    return 0;
  }
}
