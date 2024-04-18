package org.hpcclab.oprc.cli.command.pkg;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.service.WebRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
  @CommandLine.Option(names = {"--override-package", "-p"})
  String overridePackageName;
  @Inject
  WebRequester webRequester;

  @Override
  public Integer call() throws Exception {

    var pkg = Files.readString(pkgFile.toPath());
    YAMLMapper yamlMapper = new YAMLMapper();
    var map = yamlMapper.readValue(pkg, Map.class);
    JsonObject pkgJson = new JsonObject(map);

    String pkgName;
    if (overridePackageName!=null && !overridePackageName.isEmpty()) {
      pkgName = overridePackageName;
    }
    else {
      pkgName = pkgJson.getString("name");
    }
    List<String> clsNameList = new ArrayList<>();
    JsonArray classes = pkgJson.getJsonArray("classes");
    for (int i = 0; i < classes.size(); i++) {
      clsNameList.add(classes.getJsonObject(i).getString("name"));
    }

    for (String cls : clsNameList) {
      String clsKey = pkgName + '.' + cls;
      System.out.print("deleting class '" + clsKey + "'...");
      var js = deleteCls(clsKey);
      if (js!=null)
        System.out.println("succeed");
      else
        System.out.println("failed");
    }
    return 0;
  }


  public JsonObject deleteCls(String key) throws IOException {
    return webRequester.pmDelete(
      UriTemplate.of("/api/classes/{+cls}")
        .expandToString(Variables.variables()
          .set("cls", key)
        )
    );
  }

}
