package pawissanutt.oprc.cli.mixin;

import lombok.Data;
import picocli.CommandLine;

@Data
public class S3Mixin {
    @CommandLine.Option(
            names = {"--s3-url"},
            defaultValue = "${env:S3_URL:-http://localhost:9000}"
    )
    String s3Url;

    @CommandLine.Option(
            names = {"--s3-bkt"},
            defaultValue = "${env:S3_BKT:-kn-bkt}"
    )
    String bucket;

    @CommandLine.Option(
            names = "--s3-sec",
            defaultValue = "${env:S3_SECRET}"
    )
    String secret;
    @CommandLine.Option(
            names = "--s3-acc",
            defaultValue = "${env:S3_ACCESS}"
    )
    String access;
}
