package org.hpcclab.oprc.cli.command.dev;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
  name = "function-list",
  aliases = {"fl"},
  description = "List functions",
  mixinStandardHelpOptions = true
)
public class DevFnListCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevFnListCommand.class);
  @CommandLine.Mixin
  CommonOutputMixin commonOutputMixin;


  @Inject
  LocalDevManager devManager;
  @Inject
  OutputFormatter outputFormatter;

  @CommandLine.Parameters(defaultValue = "")
  String fnKey;

  @Override
  public Integer call() throws Exception {
    List<OFunction> clsList;
    if (fnKey.isEmpty()) {
      clsList = devManager.getFnRepo().getMap().stream().toList();
    } else {
      clsList = List.of(devManager.getFnRepo().get(fnKey));
    }
    Object[] objects = clsList.stream().map(JsonObject::mapFrom).toArray();
    JsonArray jsonArray = JsonArray.of(objects);
    outputFormatter.print(commonOutputMixin.getOutputFormat(), jsonArray);
    return 0;
  }
}
