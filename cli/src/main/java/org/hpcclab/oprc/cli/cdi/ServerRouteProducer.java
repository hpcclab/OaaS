package org.hpcclab.oprc.cli.cdi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.hpcclab.oaas.invocation.InvocationReqHandler;
import org.hpcclab.oaas.invocation.LocationAwareInvocationForwarder;
import org.hpcclab.oaas.invocation.service.VertxInvocationRoutes;
import org.hpcclab.oaas.invocation.service.VertxPackageRoutes;
import org.hpcclab.oaas.mapper.ProtoMapperImpl;
import org.hpcclab.oaas.model.cls.OClass;
import org.hpcclab.oaas.model.function.OFunction;
import org.hpcclab.oaas.model.pkg.OPackage;
import org.hpcclab.oaas.repository.ClassResolver;
import org.hpcclab.oaas.repository.PackageDeployer;
import org.hpcclab.oaas.repository.PackageValidator;
import org.hpcclab.oprc.cli.state.LocalDevManager;

/**
 * @author Pawissanutt
 */
@ApplicationScoped
public class ServerRouteProducer {
  @Produces
  @ApplicationScoped
  VertxInvocationRoutes vertxInvocationService(LocationAwareInvocationForwarder invocationForwarder,
                                               InvocationReqHandler invocationReqHandler,
                                               ObjectMapper mapper) {
    return new VertxInvocationRoutes(
      invocationForwarder,
      invocationReqHandler,
      mapper
    );
  }

  @Produces
  @ApplicationScoped
  VertxPackageRoutes vertxPackageService(LocalDevManager devManager) {
    return new VertxPackageRoutes(
      devManager.getClsRepo(),
      devManager.getFnRepo(),
      new PackageValidator(devManager.getFnRepo()),
      new ClassResolver(devManager.getClsRepo()),
      new ProtoMapperImpl(),
      new PackageDeployer() {
        @Override
        public void deploy(OPackage pkg) {

        }

        @Override
        public void detach(OClass cls) {

        }

        @Override
        public void detach(OFunction fn) {

        }
      }
    );
  }
}
