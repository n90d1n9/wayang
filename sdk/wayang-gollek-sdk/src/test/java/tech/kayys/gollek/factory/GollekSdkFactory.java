package tech.kayys.gollek.factory;

import java.util.List;
import java.util.ArrayList;

/**
 * Test stub for GollekSdkFactory used by WayangGollekFacade unit tests.
 */
public class GollekSdkFactory {

    public static Object createLocalSdk() {
        return new TestSdk();
    }

    public static class TestSdk {
        public List<String> listModels() {
            List<String> l = new ArrayList<>();
            l.add("test-model-1");
            l.add("test-model-2");
            return l;
        }

        public java.util.Optional<String> getModelInfo(String modelId) {
            if (modelId != null && modelId.contains("test-model")) {
                return java.util.Optional.of("info:" + modelId);
            }
            return java.util.Optional.empty();
        }

        public void pullModel(String modelSpec, java.util.function.Consumer<Object> progress) {
            if (progress != null) progress.accept("pulled:" + modelSpec);
        }

        public void pullModel(String modelSpec) {
            // no-op
        }
    }
}
