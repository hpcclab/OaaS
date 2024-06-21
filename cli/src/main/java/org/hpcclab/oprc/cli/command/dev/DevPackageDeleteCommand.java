package org.hpcclab.oprc.cli.command.dev;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.repository.PackageValidator;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author Pawissanutt
 */
@CommandLine.Command(name = "package-delete",
  aliases = {"pd"},
  description = "Delete package from the local state",
  mixinStandardHelpOptions = true
)
public class DevPackageDeleteCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevPackageDeleteCommand.class);
  @CommandLine.Parameters()
  File pkgFile;

  @Inject
  LocalDevManager devManager;
  @Inject
  ConfigFileManager fileManager;

  @Override
  public Integer call() throws Exception {
    FileCliConfig.LocalDevelopment localDev = fileManager.getOrCreate().getLocalDev();
    logger.debug("use {}", localDev);
    var yamlMapper = new YAMLMapper();
    var pkg = yamlMapper.readValue(pkgFile, OPackage.class);
    PackageValidator validator = new PackageValidator(devManager.getFnRepo());
    pkg = validator.validate(pkg);
    for (OClass cls : pkg.getClasses()) {
      devManager.getClsRepo().remove(cls.getKey());
      System.out.println("delete class: " + cls.getKey());
    }
    for (OFunction function : pkg.getFunctions()) {
      devManager.getFnRepo().remove(function.getKey());
      System.out.println("delete func: " + function.getKey());
    }
    devManager.persistPkg();
    return 0;
  }
}
