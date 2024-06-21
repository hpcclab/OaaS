package org.hpcclab.oprc.cli.command.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.hpcclab.oaas.invocation.InvocationManager;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.controller.fn.logical.NewFnController;
import org.hpcclab.oaas.model.invocation.InvocationRequest;
import org.hpcclab.oaas.model.invocation.InvocationResponse;
import org.hpcclab.oaas.model.object.JsonBytes;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "object-create",
  aliases = {"oc", "o"},
  description = "create an object",
  mixinStandardHelpOptions = true
)
@RegisterForReflection(
  targets = {
    NewFnController.ObjectConstructRequest.class,
    NewFnController.ObjectConstructResponse.class
  }
)
public class DevObjectCreateCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevObjectCreateCommand.class);
  @CommandLine.Parameters(defaultValue = "")
  String cls;

  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;
  @CommandLine.Option(names = {"-d", "--data"})
  String data;

  @CommandLine.Option(names = {"-f", "--files"})
  Map<String, File> files;

  @CommandLine.Option(names = "--fb", defaultValue = "new")
  String fb;

  @CommandLine.Option(names = {"-s", "--save"}, description = "save the object id to config file")
  boolean save;

  @Inject
  ConfigFileManager fileManager;
  @Inject
  InvocationManager invocationManager;
  @Inject
  OutputFormatter outputFormatter;
  @Inject
  LocalDevManager devManager;

  @Override
  public Integer call() throws Exception {
    var conf = fileManager.dev();
    if (cls==null) cls = conf.getDefaultClass();
    JsonBytes body;
    if (data!=null) {
      body = new JsonBytes(data.getBytes());
    } else {
      body = JsonBytes.EMPTY;
    }
    InvocationReqHandler reqHandler = invocationManager.getReqHandler();
    var constructRequest = NewFnController.ObjectConstructRequest.of(body.getNode());
    var mapper = new ObjectMapper();
    InvocationRequest req = InvocationRequest.builder()
      .cls(cls)
      .fb(fb)
      .body(new JsonBytes(mapper.valueToTree(constructRequest)))
      .build();
    logger.debug("cls {}", invocationManager.getRegistry().printStructure());
    InvocationResponse resp = reqHandler.invoke(req).await().indefinitely();
    outputFormatter.print(commonOutputMixin.getOutputFormat(), JsonObject.mapFrom(resp));
    devManager.persistObject();
    var out = resp.output();
    if (save && out!=null && !out.getKey().isEmpty()) {
      conf.setDefaultObject(out.getKey());
      String outCls = out.getMeta().getCls();
      conf.setDefaultClass(outCls);
      fileManager.update(conf);
    }
    return 0;
  }
}
