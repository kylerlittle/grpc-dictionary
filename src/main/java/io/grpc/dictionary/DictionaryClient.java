package io.grpc.dictionary;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.dictionary.DictionaryGrpc.DictionaryBlockingStub;
import io.grpc.dictionary.DictionaryGrpc.DictionaryStub;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// use for input
import java.util.Scanner;

/**
 * Client code that makes gRPC calls to the server.
 */
public class DictionaryClient {
  private static final Logger logger = Logger.getLogger(DictionaryClient.class.getName());

  // channel provides a connection to a gRPC server on a specified host and port
  // and is used when creating a client stub (or just “client” in some languages)
  private final ManagedChannel channel;

  // RPC call waits for the server to respond, and will either return a response
  // or raise an exception.
  private final DictionaryBlockingStub blockingStub;

  // makes non-blocking calls to the server, where the response is returned
  // asynchronously
  private final DictionaryStub asyncStub;

  /** Construct client for accessing Dictionary server at {@code host:port}. */
  public DictionaryClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  /**
   * Construct client for accessing Dictionary server using the existing channel.
   */
  public DictionaryClient(ManagedChannelBuilder<?> channelBuilder) {
    channel = channelBuilder.build();
    blockingStub = DictionaryGrpc.newBlockingStub(channel);
    asyncStub = DictionaryGrpc.newStub(channel);
  }

  /**
   * Request to close the gRPC channel -- fails if not accomplished
   * before timeout.
   * @throws InterruptedException
   */
  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Blocking unary call -- i.e. block the current thread & wait
   * for response. Raise exception if failure.
   */
  public void lookUpWord(String wordToLookUp, String ISOLanguageCode) {
    WordRequest request = WordRequest.newBuilder()
      .setWord(wordToLookUp)
      .setISOCode(ISOLanguageCode)
      .build();

    LookUpResponse resp;
    try {
      resp = blockingStub.lookUpWord(request);
    } catch (StatusRuntimeException e) {
      warning("RPC failed: {0}", e.getStatus());
      return;
    }

    // Display result!
    if (resp.getCode() == ResponseCode.SUCCESS) {
      System.out.println("Definition: " + resp.getDefinition());
      System.out.println("Encoding: " + resp.getISOCode());
    } else {
      System.out.println("Failed to find word in dictionary");
    }
  }

  /** Issues several different requests and then exits. */
  public static void main(String[] args) throws InterruptedException {
    Scanner scan = new Scanner(System.in);
    String inputStr = null;
    DictionaryClient client = new DictionaryClient("localhost", 8980);

    try {
      while (true) {
        System.out.print("Enter a word to look up: ");

        // get input
        inputStr = scan.nextLine();

        // If user entered something
        if (inputStr != null && inputStr.length() > 0) {
          // Look up the word in the dictionary
          client.lookUpWord(inputStr, "en");
        } else {
          break;
        }
      }
    } finally {
      client.shutdown();
    }

    // close scanner
    scan.close();
  }

  private void warning(String msg, Object... params) {
    logger.log(Level.WARNING, msg, params);
  }
}
