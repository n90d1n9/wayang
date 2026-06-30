package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@Command(name = "serve",
        description = "Start Wayang server (gRPC and/or REST).",
        mixinStandardHelpOptions = true)
public class WayangServeCommand implements Callable<Integer> {

    @ParentCommand WayangGollekCli parent;

    @Option(names = {"--rest"}, description = "Start REST server only")
    boolean restOnly;

    @Option(names = {"--grpc"}, description = "Start gRPC server only")
    boolean grpcOnly;

    @Option(names = {"--rest-port"}, description = "REST port (default 8080)")
    Integer restPort;

    @Option(names = {"--grpc-port"}, description = "gRPC port (default 50051)")
    Integer grpcPort;

    @Override
    public Integer call() throws Exception {
        java.util.List<String> args = new java.util.ArrayList<>();
        if (restOnly && !grpcOnly) args.add("--rest");
        else if (grpcOnly && !restOnly) args.add("--grpc");
        if (restPort != null) args.add("--rest-port=" + restPort);
        if (grpcPort != null) args.add("--grpc-port=" + grpcPort);

        String[] arr = args.toArray(new String[0]);

        try {
            // Use reflection to avoid compile-time dependency on wayang-api-grpc module.
            Class<?> cls = Class.forName("tech.kayys.wayang.api.grpc.WayangServe");
            Method main = cls.getMethod("main", String[].class);
            main.invoke(null, (Object) arr);
            return 0;
        } catch (ClassNotFoundException cnf) {
            String msg = "WayangServe class not available in classpath. Build or add wayang-api-grpc module to run the server.";
            if (parent != null && parent.context() != null) parent.context().out().println(msg);
            else System.err.println(msg);
            return 2;
        } catch (Throwable t) {
            t.printStackTrace(parent == null ? System.err : parent.context().out());
            return 1;
        }
    }
}
