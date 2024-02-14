package org.hpcclab.oprc.cli.command.obj;

import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.hpcclab.oprc.cli.mixin.OaasMixin;
import org.hpcclab.oprc.cli.service.OaasObjectCreator;
import org.hpcclab.oprc.cli.service.OutputFormatter;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "create",
        aliases = "c",
        mixinStandardHelpOptions = true
)
public class ObjectCreateCommand implements Callable<Integer> {
    @CommandLine.Mixin
    OaasMixin oaasMixin;
    @CommandLine.Parameters(defaultValue = "example.record")
    String cls;

    @CommandLine.Mixin
    CommonOutputMixin commonOutputMixin;
    @CommandLine.Option(names = {"-d", "--data"})
    String data;

    @CommandLine.Option(names = {"-f", "--files"})
    Map<String, File> files;

    @CommandLine.Option(names = "--fb", defaultValue = "new")
    String fb;

    @Inject
    OaasObjectCreator oaasObjectCreator;
    @Inject
    OutputFormatter outputFormatter;

    @Override
    public Integer call() throws Exception {
        oaasObjectCreator.setOaasMixin(oaasMixin);
        var res = oaasObjectCreator.createObject(cls, data!=null ? new JsonObject(data):null, fb, files);
        outputFormatter.print(commonOutputMixin.getOutputFormat(), res);
        return 0;
    }
}
