package org.hpcclab.oprc.cli.command.dev;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.Dataflows;
import org.hpcclab.oaas.model.function.FunctionBinding;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.model.provision.ProvisionConfig;
import org.hpcclab.oaas.model.state.StateSpecification;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.PackageValidator;
import org.hpcclab.oprc.cli.state.LocalStateManager;
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
  registerFullHierarchy=true
)
public class DevPackageApplyCommand implements Callable<Integer> {

  @CommandLine.Parameters()
  File pkgFile;

  @Inject
  LocalStateManager localStateManager;

  @Override
  public Integer call() throws Exception {
    ClassResolver classResolver = new ClassResolver(localStateManager.getClsRepo());
    PackageValidator validator = new PackageValidator(localStateManager.getFnRepo());
    var yamlMapper = new YAMLMapper();
    var pkg = yamlMapper.readValue(pkgFile, OPackage.class);
    var validatedPkg = validator.validate(pkg);
    var classes = validatedPkg.getClasses();
    var functions = validatedPkg.getFunctions();
    var clsMap = classes.stream()
      .collect(Collectors.toMap(OClass::getKey, Function.identity()));
    var changedClasses = classResolver.resolveInheritance(clsMap);
    for (OClass cls : changedClasses.values()) {
      System.out.println("update cls: " + cls.getKey());
    }
    localStateManager.getClsRepo().persist(changedClasses.values().stream().toList());
    localStateManager.getFnRepo().persist(functions);
    localStateManager.persist();
    return 0;
  }
}
