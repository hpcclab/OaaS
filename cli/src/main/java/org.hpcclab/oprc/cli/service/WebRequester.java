package org.hpcclab.oprc.cli.service;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hpcclab.oprc.cli.mixin.CommonOutputMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class WebRequester {
    private static final Logger logger = LoggerFactory.getLogger( WebRequester.class );
    @Inject
    WebClient webClient;
    @Inject
    OutputFormatter outputFormatter;


    public JsonObject get(String url) {
        var res = webClient.getAbs(url)
                .sendAndAwait();
        if (res.statusCode() != 200){
            logger.error("error response: code={}", res.statusCode());
            return null;
        }
        return res.bodyAsJsonObject();
    }

    public int getAndPrint(String url, CommonOutputMixin.OutputFormat format) {
        var jsonObject = get(url);
        if (jsonObject == null)
            return 1;
        outputFormatter.print(format, jsonObject);
        return 0;
    }
}
