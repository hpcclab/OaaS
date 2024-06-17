package org.hpcclab.oprc.cli.command.obj;

import com.jayway.jsonpath.DocumentContext;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.JsonUtil;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OaasObjectCreator;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "create",
  aliases = "c",
  description = "create an object",
  mixinStandardHelpOptions = true
)
public class ObjectCreateCommand implements Callable<Integer> {

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
  OaasObjectCreator oaasObjectCreator;
  @Inject
  OutputFormatter outputFormatter;

  @Override
  public Integer call() throws Exception {
    try {
      FileCliConfig.FileCliContext current = fileManager.current();
      oaasObjectCreator.setConf(current);
      if (cls.isBlank())
        cls = current.getDefaultClass();
      JsonObject res = oaasObjectCreator.createObject(cls, data!=null ? new JsonObject(data):null, fb, files);

      outputFormatter.print(commonOutputMixin.getOutputFormat(), res);
      DocumentContext doc = JsonUtil.parse(res.toString());
      if (save) {
        String id = doc.read("$.output._meta.id", String.class);
        String outCls = doc.read("$.output._meta.cls");
        current.setDefaultObject(id);
        current.setDefaultClass(outCls);
        fileManager.update(current);
      }
      return 0;
    } catch (Exception e) {
      System.err.println(e);
      return 1;
    }
  }
}
