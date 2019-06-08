package io.grpc.dictionary;

import com.google.protobuf.util.JsonFormat;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Common utilities for the Dictionary Server code.
 */
public class DictionaryUtil {
  /**
   * Gets the default features file from classpath.
   */
  public static URL getDefaultFeaturesFile() {
    return DictionaryServer.class.getResource("dictionary.json");
  }

  /**
   * Parses the JSON input file containing the list of word entries.
   */
  public static List<WordRequest> parseFeatures(URL file) throws IOException {
    InputStream input = file.openStream();
    try {
      Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
      try {
        DictionaryDatabase.Builder database = DictionaryDatabase.newBuilder();
        JsonFormat.parser().merge(reader, database);
        return database.getDictionaryList();
      } finally {
        reader.close();
      }
    } finally {
      input.close();
    }
  }

  /**
   * Add word to JSON file specified by URL.
   * Only adds word if not already present in the file.
   * 
   * Return true if successful add; else false
   * @param file
   * @return
   * @throws IOException
   */
  public static boolean addWord(URL file, WordRequest wordToAdd) throws IOException {
    // First, check if word is present in dictionary already.
    InputStream input = file.openStream();
    List<WordRequest> wordList;
    boolean result;

    try {
      Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
      try {
        DictionaryDatabase.Builder database = DictionaryDatabase.newBuilder();
        JsonFormat.parser().merge(reader, database);
        wordList = database.getDictionaryList();

        // Present? Assumes "contains" operates on all fields of object.
        if (wordList.contains(wordToAdd)) {
          result = false;
        } else {
          result = true;
          
          // Close input streams
          reader.close();
          input.close();

          // Word not present in dictionary, so add it!
          URLConnection connection = file.openConnection();
          connection.setDoOutput(true);
          OutputStream outputStream = connection.getOutputStream();
          try {
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
            try {
              JsonFormat.printer().appendTo(wordToAdd, outputWriter);
            } finally {
              outputWriter.close();
            }
          } finally {
            outputStream.close();
          }   
        }
      } finally {
        reader.close();
      }
    } finally {
      input.close();
    }

    return result;
  }
}
