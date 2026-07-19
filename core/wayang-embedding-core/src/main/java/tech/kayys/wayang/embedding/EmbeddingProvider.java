package tech.kayys.wayang.embedding;

import java.util.List;

public interface EmbeddingProvider {

    String name();

    boolean supports(String model);

    List<float[]> embedAll(List<String> inputs, String model);
}
