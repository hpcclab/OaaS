package org.hpcclab.oprc.cli.command.fn;

import io.vertx.mutiny.ext.web.client.WebClient;
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
        name = "list",
        aliases = {"l"},
        mixinStandardHelpOptions = true
)
public class FnListCommand implements Callable<Integer> {
    private static final Logger logger = LoggerFactory.getLogger( FnListCommand.class );
    @CommandLine.Mixin
    OaasMixin oaasMixin;
    @CommandLine.Mixin
    CommonOutputMixin commonOutputMixin;
    @Inject
    WebRequester webRequester;
    @CommandLine.Parameters(defaultValue = "")
    String fn;


    @Override
    public Integer call() throws Exception {
        var oc = oaasMixin.getOcUrl();
        return webRequester.getAndPrint(
                UriTemplate.of("{+oc}/api/functions/{+fn}")
                        .expandToString(Variables.variables()
                                .set("oc", oc)
                                .set("fn", fn)
                        ),
                commonOutputMixin.getOutputFormat()
        );
    }
}
