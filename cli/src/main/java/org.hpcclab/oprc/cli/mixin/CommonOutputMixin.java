package org.hpcclab.oprc.cli.mixin;

import lombok.Data;
import picocli.CommandLine;

@Data
public class CommonOutputMixin {


    @CommandLine.Option(
            names = {"-o", "--output"},
            description = "Format of output",
            defaultValue = "YAML"
    )
    OutputFormat outputFormat;

    public enum OutputFormat{
        JSON, NDJSON, YAML, PJSON
    }
}
