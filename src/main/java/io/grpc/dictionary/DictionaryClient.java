package io.grpc.dictionary;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.dictionary.DictionaryGrpc.DictionaryBlockingStub;
import io.grpc.dictionary.DictionaryGrpc.DictionaryStub;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.NoSuchElementException;
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

  /**
   * Blocking unary call -- i.e. block the current thread & wait
   * for response. Raise exception if failure.
   */
  public void defineWord(String wordToLookUp, String ISOLanguageCode, String definition) {
    WordRequest request = WordRequest.newBuilder()
      .setWord(wordToLookUp)
      .setISOCode(ISOLanguageCode)
      .setDefinition(definition)
      .build();

    DefineResponse resp;
    try {
      resp = blockingStub.defineWord(request);
    } catch (StatusRuntimeException e) {
      warning("RPC failed: {0}", e.getStatus());
      return;
    }

    // Display result
    if (resp.getCode() == ResponseCode.SUCCESS) {
      System.out.println("Successfully added word to dictionary");
    } else {
      System.out.println("Failed to add word to dictionary");
    }
  }

  /** Issues several different requests and then exits. */
  public static void main(String[] args) throws InterruptedException {
    Scanner scan = new Scanner(System.in);
    String word = null, ISOcode = null, definition;
    DictionaryClient client = new DictionaryClient("localhost", 8980);
    int optionSelected = 0;

    try {
      while (true) {
        // Get user option
        System.out.print("Enter '1' to look up a word or '2' to define a word: ");
        optionSelected = scan.nextInt();

        // Get a word to look up from user
        System.out.print("Enter a word: ");
        word = scan.nextLine();

        // Get an encoding to look up from user
        System.out.print("Enter the 2-letter ISO language code: ");
        ISOcode = scan.nextLine();

        switch (optionSelected) {
          case 1:
            // Look up the word
            client.lookUpWord(word, ISOcode);
            break;
          case 2:
            // Get the definition
            System.out.print(String.format("Enter the definition for '%s': ", word));
            definition = scan.nextLine();

            // Define the word
            client.defineWord(word, ISOcode, definition);
            break;
          default:
            System.out.println("Invalid option. Try again.");
            break;
        }
      }
    } catch (NoSuchElementException ex) {
      // successfully exit infinite loop when nothing is entered
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
