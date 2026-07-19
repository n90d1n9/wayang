package tech.kayys.wayang.api.grpc;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.sdk.gollek.ProjectStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GrpcSessionService extends SessionServiceGrpc.SessionServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(GrpcSessionService.class);
    private final ProjectStore store;

    public GrpcSessionService() {
        try {
            this.store = new ProjectStore(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ProjectStore", e);
        }
    }

    @Override
    public void appendTranscript(AppendTranscriptRequest req, StreamObserver<AppendTranscriptResponse> resp) {
        try {
            var entry = req.getEntry();
            // Map to a simple structure the ProjectStore expects (list of maps)
            Map<String,Object> m = Map.of(
                    "role", entry.getAuthor(),
                    "text", entry.getContent(),
                    "timestamp", entry.getTimestamp()
            );
            List<Object> list = new ArrayList<>();
            list.add(m);
            // load existing then append
            @SuppressWarnings({"rawtypes","unchecked"})
            java.util.List existing = store.loadTranscript(req.getProjectId(), req.getSessionId());
            java.util.List merged = new ArrayList(existing == null ? java.util.Collections.emptyList() : existing);
            merged.addAll(list);
            store.saveTranscript(req.getProjectId(), req.getSessionId(), merged);
            AppendTranscriptResponse r = AppendTranscriptResponse.newBuilder().setStatus(OperationResponse.newBuilder().setOk(true).setMessage("saved").build()).setSaved(entry).build();
            resp.onNext(r);
            resp.onCompleted();
        } catch (Exception e) {
            log.error("appendTranscript failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getTranscript(ListSessionsRequest req, StreamObserver<TranscriptEntry> resp) {
        try {
            java.util.List<String> sessionIds = store.listSessions(req.getProjectId());
            if (sessionIds == null) sessionIds = java.util.Collections.emptyList();
            int idx = 0;
            for (String sid : sessionIds) {
                java.util.List entries = store.loadTranscript(req.getProjectId(), sid);
                if (entries == null) entries = java.util.Collections.emptyList();
                for (Object o : entries) {
                tech.kayys.wayang.api.grpc.TranscriptEntry.Builder b = tech.kayys.wayang.api.grpc.TranscriptEntry.newBuilder();
                if (o instanceof Map) {
                    Map<?,?> mp = (Map<?,?>) o;
                    Object role = mp.containsKey("role") ? mp.get("role") : (mp.containsKey("type") ? mp.get("type") : "assistant");
                    Object text = mp.containsKey("text") ? mp.get("text") : (mp.containsKey("content") ? mp.get("content") : mp.toString());
                    b.setAuthor(String.valueOf(role));
                    b.setContent(String.valueOf(text));
                    b.setTimestamp(mp.containsKey("timestamp") ? Long.parseLong(String.valueOf(mp.get("timestamp"))) : System.currentTimeMillis());
                } else if (o instanceof String) {
                    b.setAuthor("assistant");
                    b.setContent((String) o);
                    b.setTimestamp(System.currentTimeMillis());
                } else {
                    b.setAuthor("assistant");
                    b.setContent(String.valueOf(o));
                    b.setTimestamp(System.currentTimeMillis());
                }
                b.setId("t-" + idx++);
                resp.onNext(b.build());
                }
            }
            resp.onCompleted();
        } catch (Exception e) {
            log.error("getTranscript failed", e);
            resp.onError(io.grpc.Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
