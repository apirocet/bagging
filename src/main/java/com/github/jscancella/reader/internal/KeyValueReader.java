package com.github.jscancella.reader.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import com.github.jscancella.exceptions.InvalidBagMetadataException;

/**
 * Convenience class for reading key value pairs from a file
 */
public enum KeyValueReader {;//using enum to enforce singleton
  private static final Logger logger = LoggerFactory.getLogger(KeyValueReader.class);
  private static final String INDENTED_LINE_REGEX = "^\\s+.*";
  private static final ResourceBundle messages = ResourceBundle.getBundle("MessageBundle");
  private static final int PARSED_LINE_LENGTH = 2; //since it is key: value there should only be two items per line after parsing

  /**
   * Generic method to read key value pairs from the bagit files, like bagit.txt or bag-info.txt
   * 
   * @param file the file to read
   * @param splitRegex how to split the key from the value
   * @param charset the encoding of the file
   * 
   * @return a list of key value pairs
   * 
   * @throws IOException if there was a problem reading the file
   * @throws InvalidBagMetadataException if the file does not conform to pattern of key value
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  public static List<SimpleImmutableEntry<String, String>> 
    readKeyValuesFromFile(final Path file, final String splitRegex, final Charset charset) throws IOException{
    
    final List<SimpleImmutableEntry<String, String>> keyValues = new ArrayList<>();
    
    try(BufferedReader reader = Files.newBufferedReader(file, charset)){
      String line = reader.readLine();
      while(line != null){
        if(line.matches(INDENTED_LINE_REGEX) && !keyValues.isEmpty()){
          mergeIndentedLine(line, keyValues);
        }
        else{
          final String[] parts = checkLineFormat(line, splitRegex);
          final String key = parts[0].trim();
          final String value = parts[1].trim();
          logger.debug(messages.getString("read_key_value_line"), key, value, file, splitRegex);
          keyValues.add(new SimpleImmutableEntry<>(key, value));
        }
         
        line = reader.readLine();
      }
    }
    
    return keyValues;
  }
  
  /*
   * If a line is indented, it actually belongs with the previous line so we need to merge them together
   */
  private static void mergeIndentedLine(final String line, final List<SimpleImmutableEntry<String, String>> keyValues){
    final SimpleImmutableEntry<String, String> oldKeyValue = keyValues.remove(keyValues.size() -1);
    final SimpleImmutableEntry<String, String> newKeyValue = new SimpleImmutableEntry<>(oldKeyValue.getKey(), oldKeyValue.getValue() + System.lineSeparator() +line);
    keyValues.add(newKeyValue);
    
    logger.debug(messages.getString("found_indented_line"), oldKeyValue.getKey());
  }
  
  private static String[] checkLineFormat(final String line, final String splitRegex){
    final String[] parts = line.split(splitRegex, PARSED_LINE_LENGTH);
    
    if(parts.length != PARSED_LINE_LENGTH){
      final String formattedMessage = messages.getString("malformed_key_value_line_error");
      throw new InvalidBagMetadataException(MessageFormatter.format(formattedMessage, line, splitRegex).getMessage());
    }
    
    return parts;
  }
}
