package org.hpcclab.oprc.cli.mixin;

import lombok.Data;
import org.hpcclab.oprc.cli.conf.OutputFormat;
import picocli.CommandLine;

@Data
public class CommonOutputMixin {
    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Format of output",
            defaultValue = "YAML"
    )
    OutputFormat outputFormat;

}
