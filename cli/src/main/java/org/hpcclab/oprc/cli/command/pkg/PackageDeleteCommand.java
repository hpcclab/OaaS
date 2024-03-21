package org.hpcclab.oprc.cli.command.pkg;

import com.jayway.jsonpath.JsonPath;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import org.hpcclab.oprc.cli.service.WebRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(
  name = "delete",
  aliases = {"d", "rm", "r"},
  description = "Delete a package",
  mixinStandardHelpOptions = true
)
public class PackageDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(PackageDeleteCommand.class);
  @CommandLine.Parameters()
  File pkgFile;
  @Inject
  WebRequester webRequester;

  @Override
  public Integer call() throws Exception {

    var pkg = Files.readString(pkgFile.toPath());

    String pkgName = JsonPath.read(pkg, "$.name");
    List<String> clsNameList = JsonPath.read(pkg, "$.classes[*].name");

    for (String cls : clsNameList) {
      String clsKey = pkgName + '.' + cls;
      System.out.println("deleting class '" + clsKey + "'...");
      deleteCls(clsKey);
      System.out.println("class '" + clsKey + "' is deleted");
    }
    return 0;
  }


  public void deleteCls(String key) throws IOException {
    webRequester.pmDelete(
      UriTemplate.of("/api/classes/{+cls}")
        .expandToString(Variables.variables()
          .set("cls", key)
        )
    );
  }

}
