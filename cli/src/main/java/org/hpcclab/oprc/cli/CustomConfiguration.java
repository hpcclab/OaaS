package org.hpcclab.oprc.cli;


import io.quarkus.picocli.runtime.PicocliCommandLineFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import picocli.CommandLine;

@ApplicationScoped
class CustomConfiguration {
    @Produces
    CommandLine customCommandLine(PicocliCommandLineFactory factory) {
        return factory.create().setCaseInsensitiveEnumValuesAllowed(true);
    }
}
