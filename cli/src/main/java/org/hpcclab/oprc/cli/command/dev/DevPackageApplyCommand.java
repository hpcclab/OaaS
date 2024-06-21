package org.hpcclab.oprc.cli.command.dev;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.*;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.PackageValidator;
import org.hpcclab.oprc.cli.conf.ConfigFileManager;
import org.hpcclab.oprc.cli.conf.FileCliConfig;
import org.hpcclab.oprc.cli.state.LocalDevManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Pawissanutt
 */
@CommandLine.Command(name = "package-apply",
  aliases = {"pa", "p"},
  description = "Apply package to the local state",
  mixinStandardHelpOptions = true
)
@RegisterForReflection(
  targets = {
    OPackage.class,
    OFunction.class,
    OClass.class,
    Dataflows.class,
    ProvisionConfig.class,
    FunctionBinding.class,
    StateSpecification.class
  },
  registerFullHierarchy = true
)
public class DevPackageApplyCommand implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(DevPackageApplyCommand.class);
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
    ClassResolver classResolver = new ClassResolver(devManager.getClsRepo());
    PackageValidator validator = new PackageValidator(devManager.getFnRepo());
    var yamlMapper = new YAMLMapper();
    var pkg = yamlMapper.readValue(pkgFile, OPackage.class);
    var validatedPkg = validator.validate(pkg);
    var classes = validatedPkg.getClasses();
    var functions = validatedPkg.getFunctions();
    var clsMap = classes.stream()
      .collect(Collectors.toMap(OClass::getKey, Function.identity()));
    var changedClasses = classResolver.resolveInheritance(clsMap);
    for (OFunction function : functions) {
      if (function.getType()!=FunctionType.TASK) continue;
      function.setStatus(new OFunctionDeploymentStatus()
        .setCondition(DeploymentCondition.RUNNING)
        .setInvocationUrl(localDev.fnDevUrl())
      );
      logger.debug("func {}", function);
    }
    devManager.getClsRepo().persist(changedClasses.values().stream().toList());
    devManager.getFnRepo().persist(functions);
    for (OClass cls : changedClasses.values()) {
      System.out.println("update class: " + cls.getKey());
    }
    for (OFunction fn : functions) {
      System.out.println("update func: " + fn.getKey());
    }
    devManager.persistPkg();
    return 0;
  }
}
