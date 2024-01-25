package org.hpcclab.oprc.cli.command.orbit;

import io.vertx.mutiny.uritemplate.UriTemplate;
import io.vertx.mutiny.uritemplate.Variables;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.mixin.OaasMixin;
import org.hpcclab.oprc.cli.service.WebRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "delete orbit",
        aliases = {"d", "delete"},
        mixinStandardHelpOptions = true
)
public class OrbitDeleteCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger( OrbitDeleteCommand.class );
    @CommandLine.Mixin
    OaasMixin oaasMixin;
    @CommandLine.Mixin
    CommonOutputMixin commonOutputMixin;
    @Inject
    WebRequester webRequester;

    @CommandLine.Parameters(defaultValue = "")
    String orbitId;

    @Override
    public Integer call() throws Exception {
        var oc = oaasMixin.getOcUrl();
        return webRequester.deleteAndPrint(
                UriTemplate.of("{+oc}/api/orbits/{+orbitId}")
                        .expandToString(Variables.variables()
                                .set("oc", oc)
                                .set("orbitId",orbitId)
                        ),
                commonOutputMixin.getOutputFormat()
        );
    }
}
