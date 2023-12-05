package org.hpcclab.oprc.cli.mixin;

import lombok.Data;
import picocli.CommandLine;

@Data
public class OaasMixin {

    @CommandLine.Option(
            names = {"--oc"},
            description = "Base URL of oc server. Default(ENV:OPRC_OC): ${DEFAULT-VALUE}",
            defaultValue = "${env:OPRC_OC}"
    )
    private String ocUrl;


    @CommandLine.Option(
            names = {"--cds"},
            description = "Base URL of oc server. Default(ENV:OPRC_CDS): ${DEFAULT-VALUE}",
            defaultValue = "${env:OPRC_CDS}"
    )
    private String cdsUrl;

    @CommandLine.Option(
            names = {"--inv"},
            description = "Base URL of invoker server. Default(ENV:OPRC_INVOKER): ${DEFAULT-VALUE}",
            defaultValue = "${env:OPRC_INVOKER}"
    )
    private String invUrl;

    @CommandLine.Option(
            names = {"--proxy"},
            description = "The URL of proxy server. Default(ENV:OPRC_PROXY): ${DEFAULT-VALUE}",
            defaultValue = "${env:OPRC_PROXY}"
    )
    private String proxy;

    public String getOcUrl() {
        if (ocUrl != null)
            return ocUrl;
        if (cdsUrl != null)
            return cdsUrl;
        throw new IllegalArgumentException("--oc or --cds must be specified");
    }

    public String getCdsUrl() {
        if (cdsUrl != null)
            return cdsUrl;
        throw new IllegalArgumentException("--cds must be specified");
    }

    public String getInvUrl() {
        if (invUrl != null)
            return invUrl;
        if (cdsUrl != null)
            return cdsUrl;
        throw new IllegalArgumentException("--inv or --cds must be specified");
    }
}
