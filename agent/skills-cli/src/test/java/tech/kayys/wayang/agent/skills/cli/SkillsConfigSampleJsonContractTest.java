package tech.kayys.wayang.agent.skills.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsConfigSampleJsonContractTest {

    @Test
    void objectStorageConfigSampleJsonMatchesCliContract() {
        TestConsole console = new TestConsole();

        assertThat(command(console).execute("config", "sample", "rustfs", "--json")).isZero();

        assertThat(console.out().trim()).isEqualTo(objectStorageConfigSampleJson());
        assertThat(console.err()).isEmpty();
    }

    private CommandLine command(TestConsole console) {
        SkillsCommandHandler handler = SkillsCommandHandler.inMemory(console.outStream(), console.errStream());
        return new CommandLine(new SkillsCommand(handler));
    }

    private static String objectStorageConfigSampleJson() {
        return """
                {"profile":"object-storage","description":"Durable S3/RustFS-compatible object-storage profile using the configured object prefix.","propertyCount":11,"environmentCount":11,"properties":[{"key":"wayang.skills.profile","value":"object-storage","description":"Named persistence profile selector."},{"key":"wayang.skills.profile.object-prefix","value":"wayang/skills","description":"Object prefix for cloud-backed profile roles."},{"key":"wayang.storage.s3.endpoint","value":"http://localhost:9000","description":"S3/RustFS endpoint."},{"key":"wayang.storage.s3.bucket","value":"wayang","description":"S3/RustFS bucket."},{"key":"wayang.storage.s3.region","value":"us-east-1","description":"S3/RustFS region."},{"key":"wayang.storage.s3.access-key-id","value":"CHANGE_ME","description":"S3/RustFS access key id."},{"key":"wayang.storage.s3.secret-access-key","value":"CHANGE_ME","description":"S3/RustFS secret access key."},{"key":"wayang.storage.s3.path-style-access","value":"true","description":"Enable path-style access for RustFS or MinIO."},{"key":"wayang.storage.s3.path-prefix","value":"tenants/acme","description":"Bucket-wide object prefix before skill paths."},{"key":"wayang.skills.profile.max-events","value":"10000","description":"Event-history retention capacity."},{"key":"wayang.skills.lifecycle.reconcile.mode","value":"inspect-only","description":"Lifecycle state reconciliation mode."}],"environment":[{"key":"WAYANG_SKILLS_PROFILE","value":"object-storage","description":"Named persistence profile selector."},{"key":"WAYANG_SKILLS_PROFILE_OBJECT_PREFIX","value":"wayang/skills","description":"Object prefix for cloud-backed profile roles."},{"key":"WAYANG_STORAGE_S3_ENDPOINT","value":"http://localhost:9000","description":"S3/RustFS endpoint."},{"key":"WAYANG_STORAGE_S3_BUCKET","value":"wayang","description":"S3/RustFS bucket."},{"key":"WAYANG_STORAGE_S3_REGION","value":"us-east-1","description":"S3/RustFS region."},{"key":"WAYANG_STORAGE_S3_ACCESS_KEY_ID","value":"CHANGE_ME","description":"S3/RustFS access key id."},{"key":"WAYANG_STORAGE_S3_SECRET_ACCESS_KEY","value":"CHANGE_ME","description":"S3/RustFS secret access key."},{"key":"WAYANG_STORAGE_S3_PATH_STYLE_ACCESS","value":"true","description":"Enable path-style access for RustFS or MinIO."},{"key":"WAYANG_STORAGE_S3_PATH_PREFIX","value":"tenants/acme","description":"Bucket-wide object prefix before skill paths."},{"key":"WAYANG_SKILLS_PROFILE_MAX_EVENTS","value":"10000","description":"Event-history retention capacity."},{"key":"WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE","value":"inspect-only","description":"Lifecycle state reconciliation mode."}]}
                """.strip();
    }

    private static final class TestConsole {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final ByteArrayOutputStream err = new ByteArrayOutputStream();
        private final PrintStream outStream = new PrintStream(out);
        private final PrintStream errStream = new PrintStream(err);

        PrintStream outStream() {
            return outStream;
        }

        PrintStream errStream() {
            return errStream;
        }

        String out() {
            return out.toString();
        }

        String err() {
            return err.toString();
        }
    }
}
