package io.grpc.dictionary;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A gRPC server that serves the Dictionary (see dictionary_service.proto) service.
 */
public class DictionaryServer {
  private static final Logger logger = Logger.getLogger(DictionaryServer.class.getName());

  private final int port;
  private final Server server;

  public DictionaryServer(int port) throws IOException {
    this(ServerBuilder.forPort(port), port);
  }

  /** Create a DictionaryService server using serverBuilder as a base and features as data. */
  public DictionaryServer(ServerBuilder<?> serverBuilder, int port) {
    this.port = port;
    server = serverBuilder.addService(new DictionaryService())
        .build();
  }

  /** Start serving requests. */
  public void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        System.err.println("*** shutting down gRPC server since JVM is shutting down");
        DictionaryServer.this.stop();
        System.err.println("*** server shut down");
      }
    });
  }

  /** Stop serving requests and shutdown resources. */
  public void stop() {
    if (server != null) {
      server.shutdown();
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  /**
   * Main method.
   */
  public static void main(String[] args) throws Exception {
    DictionaryServer server = new DictionaryServer(8980);
    server.start();
    server.blockUntilShutdown();
  }

  /**
   * Our implementation of DictionaryService service.
   * See dictionary_service.proto for details of the methods.
   */
  private static class DictionaryService extends DictionaryGrpc.DictionaryImplBase {

    /**
     * Look up a word & return its definition if found and the ISO encoding
     * with which the definition is found in.
     */
    @Override
    public void lookUpWord(WordRequest request, StreamObserver<LookUpResponse> responseObserver) {
      // TODO > look up word in JSON file
      LookUpResponse lookUpResponse = LookUpResponse.newBuilder()
        .setCode(ResponseCode.SUCCESS)
        .setDefinition("this is a definition")
        .setISOCode(request.getISOCode())
        .build();
      responseObserver.onNext(lookUpResponse);
      responseObserver.onCompleted();
    }
  }
}
