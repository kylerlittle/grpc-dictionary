/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.routeguide;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
// import io.grpc.sandbox.StringFunctions.*;
import io.grpc.sandbox.StringFunctionsGrpc.StringFunctionsBlockingStub;
import io.grpc.sandbox.StringFunctionsGrpc.StringFunctionsStub;
// import io.grpc.sandbox.StringFunctions.RouteGuideStub;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// not sure why I need to import this
import io.grpc.sandbox.*;

// use for input
import java.util.Scanner;

/**
 * Sample client code that makes gRPC calls to the server.
 */
public class RouteGuideClient {
  private static final Logger logger = Logger.getLogger(RouteGuideClient.class.getName());

  private final ManagedChannel channel;

  // RPC call waits for the server to respond, and will either return a response or raise an exception.
  private final StringFunctionsBlockingStub blockingStub;

  // makes non-blocking calls to the server, where the response is returned asynchronously
  private final StringFunctionsStub asyncStub;

  private TestHelper testHelper;

  /** Construct client for accessing RouteGuide server at {@code host:port}. */
  public RouteGuideClient(String host, int port) {
    this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
  }

  /** Construct client for accessing RouteGuide server using the existing channel. */
  public RouteGuideClient(ManagedChannelBuilder<?> channelBuilder) {
    channel = channelBuilder.build();
    blockingStub = StringFunctionsGrpc.newBlockingStub(channel);
    asyncStub = StringFunctionsGrpc.newStub(channel);
  }

  public void shutdown() throws InterruptedException {
    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
  }

  /**
   * Blocking unary call example.  Calls getFeature and prints the response.
   */
  public void capitalizeMessage(String messageToCapitalize) {
    MessageRequest request = MessageRequest.newBuilder().setMessage(messageToCapitalize).build();

    UpperMessageResponse resp;
    try {
      resp = blockingStub.capitalizeMessage(request);
      if (testHelper != null) {
        testHelper.onMessage(resp);
      }
    } catch (StatusRuntimeException e) {
      warning("RPC failed: {0}", e.getStatus());
      if (testHelper != null) {
        testHelper.onRpcError(e);
      }
      return;
    }
    if (resp != null &&  resp.getUpperMessage() != "") {
      System.out.println("ECHO: " + resp.getUpperMessage());
    }
  }

  /** Issues several different requests and then exits. */
  public static void main(String[] args) throws InterruptedException {
    Scanner scan = new Scanner(System.in);
    String inputStr = null;
    RouteGuideClient client = new RouteGuideClient("localhost", 8980);

    while (true) {
      System.out.print("Enter a command: ");

      // get input
      inputStr = scan.nextLine();

      // if user entered something
      if (inputStr != null && inputStr.length() > 0) {
        try {
          // Capitalize the string.
          client.capitalizeMessage(inputStr);
        } finally {
          client.shutdown();
        }
      } else {
        break;
      }

      // reset string
      inputStr = null;
    }

    // close scanner
    scan.close();
  }

  private void warning(String msg, Object... params) {
    logger.log(Level.WARNING, msg, params);
  }

  /**
   * Only used for helping unit test.
   */
  @VisibleForTesting
  interface TestHelper {
    /**
     * Used for verify/inspect message received from server.
     */
    void onMessage(Message message);

    /**
     * Used for verify/inspect error received from server.
     */
    void onRpcError(Throwable exception);
  }

  @VisibleForTesting
  void setTestHelper(TestHelper testHelper) {
    this.testHelper = testHelper;
  }
}
