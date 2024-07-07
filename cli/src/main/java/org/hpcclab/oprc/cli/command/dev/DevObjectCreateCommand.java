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
import org.hpcclab.oprc.cli.service.OObjectCreator;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.Set;
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
  @Inject
  OObjectCreator objCreator;

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
    var constructRequest = NewFnController.ObjectConstructRequest.builder()
      .data(body.getNode())
      .keys(files != null? files.keySet(): Set.of())
      .build();
    var mapper = new ObjectMapper();
    InvocationRequest req = InvocationRequest.builder()
      .cls(cls)
      .fb(fb)
      .body(new JsonBytes(mapper.valueToTree(constructRequest)))
      .build();
    logger.debug("cls {}", invocationManager.getRegistry().printStructure());
    InvocationResponse resp = reqHandler.invoke(req).await().indefinitely();
    var constructResp = resp.body().mapToObj(NewFnController.ObjectConstructResponse.class);
    uploadFiles(files, constructResp);
    outputFormatter.print(commonOutputMixin.getOutputFormat(), JsonObject.mapFrom(resp));
    devManager.persistObject();
    var out = resp.output();
    if (save && out!=null && !out.getKey().isEmpty()) {
      conf.setDefaultObject(out.getKey());
      String outCls = out.getMeta().getCls();
      conf.setDefaultClass(outCls);
      fileManager.updateDev(conf);
    }
    return 0;
  }

  void uploadFiles(Map<String, File> files,
                   NewFnController.ObjectConstructResponse resp) {
    if (files == null || files.isEmpty()) return;
    Map<String, String> uploadUrls = resp.uploadUrls();
    for (var entry : uploadUrls.entrySet()) {
      objCreator.uploadFile(entry.getKey(), files.get(entry.getKey()), entry.getValue());
    }
  }
}
