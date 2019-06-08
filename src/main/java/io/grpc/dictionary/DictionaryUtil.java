package io.grpc.dictionary;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
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
        return database.getWordEntryList();
      } finally {
        reader.close();
      }
    } finally {
      input.close();
    }
  }

  /**
   * Indicates whether the given word exists (i.e. is an entry in the dictionary file)
   */
  public static boolean exists(WordRequest wordRequest) {
    return wordRequest != null && !wordRequest.getWord().isEmpty();
  }
}
