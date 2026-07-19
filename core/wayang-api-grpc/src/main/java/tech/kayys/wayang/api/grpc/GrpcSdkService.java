package tech.kayys.wayang.api.grpc;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.sdk.gollek.ProjectStore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GrpcSdkService extends SdkServiceGrpc.SdkServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcSdkService.class);
    private final ProjectStore store;

    public GrpcSdkService() {
        try {
            this.store = new ProjectStore(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ProjectStore", e);
        }
    }

    @Override
    public void listProviders(Empty req, StreamObserver<ProviderList> resp) {
        try {
            ProviderList.Builder b = ProviderList.newBuilder();
            // simple local detection: check for gollek-runner-safetensor in local m2
            Path m2 = Paths.get(System.getProperty("user.home"), ".m2", "repository", "tech", "kayys", "gollek");
            Path safetensor = m2.resolve("gollek-runner-safetensor");
            if (safetensor.toFile().exists()) {
                b.addProviders(ProviderInfo.newBuilder().setId("safetensor").setName("safetensor").setDescription("Local safetensor runner detected").addCapabilities("text-generation").build());
            }
            resp.onNext(b.build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("listProviders failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void createSdk(CreateSdkRequest req, StreamObserver<CreateSdkResponse> resp) {
        try {
            // lightweight: accept "safetensor" and return sdk id
            String provider = req.getProviderId();
            if (provider == null || provider.isBlank()) provider = "safetensor";
            String sdkId = "sdk-" + provider + "-local";
            CreateSdkResponse r = CreateSdkResponse.newBuilder().setStatus(OperationResponse.newBuilder().setOk(true).setMessage("created").build()).setSdkId(sdkId).build();
            resp.onNext(r);
            resp.onCompleted();
        } catch (Exception e) {
            log.error("createSdk failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listModels(ListModelsRequest req, StreamObserver<ModelList> resp) {
        try {
            ModelList.Builder b = ModelList.newBuilder();
            // mocked list
            b.addModels(ModelInfo.newBuilder().setId("gpt-small").setName("gpt-small").setDescription("Mock model").build());
            resp.onNext(b.build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("listModels failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void sdkStream(SdkStreamRequest req, StreamObserver<CodeChunk> resp) {
        try {
            // simple echo streaming: split prompt into words and stream as chunks
            String prompt = req.getPrompt();
            if (prompt == null) prompt = "";
            String[] parts = prompt.split("\\s+");
            int i = 0;
            for (String p : parts) {
                CodeChunk c = CodeChunk.newBuilder().setText(p + " ").setSeq(i++).setDone(false).build();
                resp.onNext(c);
                try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            }
            resp.onNext(CodeChunk.newBuilder().setText("\n").setSeq(i).setDone(true).build());
            resp.onCompleted();
        } catch (Exception e) {
            log.error("sdkStream failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
