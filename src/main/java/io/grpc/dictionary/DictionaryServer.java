package io.grpc.dictionary;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * A gRPC server that serves the Dictionary (see dictionary.proto) service.
 */
public class DictionaryServer {
  private static final Logger logger = Logger.getLogger(DictionaryServer.class.getName());

  private final int port;
  private final Server server;

  public DictionaryServer(int port) throws IOException {
    this(port, DictionaryUtil.getDefaultFeaturesFile());
  }

  /**
   * Create a DictionaryServer server listening on {@code port} using
   * {@code wordFile} database.
   */
  public DictionaryServer(int port, URL wordFile) throws IOException {
    this(ServerBuilder.forPort(port), port, DictionaryUtil.parseFeatures(wordFile));
  }

  /**
   * Create a DictionaryService server using serverBuilder as a base and words as
   * data.
   */
  public DictionaryServer(ServerBuilder<?> serverBuilder, int port, List<WordRequest> words) {
    this.port = port;
    server = serverBuilder.addService(new DictionaryService(words)).build();
  }

  /** Start serving requests. */
  public void start() throws IOException {
    server.start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown
        // hook.
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
   * Await termination on the main thread since the grpc library uses daemon
   * threads.
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
   * Our implementation of DictionaryService service. See dictionary.proto for
   * details of the methods.
   */
  private static class DictionaryService extends DictionaryGrpc.DictionaryImplBase {
    private final List<WordRequest> words;

    DictionaryService(List<WordRequest> words) {
      this.words = words;
    }

    /**
     * Look up a word & return its definition if found and the ISO encoding with
     * which the definition is found in.
     */
    @Override
    public void lookUpWord(WordRequest request, StreamObserver<LookUpResponse> responseObserver) {
      // Look up word in JSON file by getting first occurrence of word -- assume no
      // duplicates
      int indexOfWord = indexInList(request);
      WordRequest wordRequest;
      ResponseCode responseCode;
      String definition = "", code = "";

      if (indexOfWord == -1) {
        responseCode = ResponseCode.FAILURE;
      } else {
        responseCode = ResponseCode.SUCCESS;
        wordRequest = words.get(indexOfWord);
        definition = wordRequest.getDefinition();
        code = wordRequest.getISOCode();
      }

      LookUpResponse lookUpResponse = LookUpResponse.newBuilder().setCode(responseCode).setDefinition(definition)
          .setISOCode(code).build();
      responseObserver.onNext(lookUpResponse);
      responseObserver.onCompleted();
    }

    @Override
    public void defineWord(WordRequest request, StreamObserver<DefineResponse> responseObserver) {
      ResponseCode responseCode;
      try {
        boolean result = DictionaryUtil.addWord(DictionaryUtil.getDefaultFeaturesFile(), request);
        responseCode = (result) ? ResponseCode.SUCCESS : ResponseCode.FAILURE;
      } catch (IOException ex) {
        logger.warning(String.format("IOException while adding word '%s' to dictionary.", request.getWord()));
        responseCode = ResponseCode.FAILURE;
      }

      DefineResponse defineResponse = DefineResponse.newBuilder().setCode(responseCode).build();
      responseObserver.onNext(defineResponse);
      responseObserver.onCompleted();
    }

    /**
     * Look for the provided WordRequest in this.words, by matching only the "word"
     * and "ISOCode" instance fields of WordRequest. Return the index of the first
     * occurrence of the word. Return -1 if not found. Ignore case!
     * 
     * @param findThisRequest
     * @return
     */
    private int indexInList(WordRequest findThisRequest) {
      int counter = 0;

      for (WordRequest wordRequest : this.words) {
        if (wordRequest.getWord().equalsIgnoreCase(findThisRequest.getWord())
            && wordRequest.getISOCode().equalsIgnoreCase(findThisRequest.getISOCode())) {
          return counter;
        } else {
          counter++;
          continue;
        }
      }

      return -1;
    }
  }
}
