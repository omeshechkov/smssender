package kg.dtg.smssender.utils;

import com.adenki.smpp.encoding.*;

import java.util.Properties;
import java.util.Random;

public class SMPPUtils {
  public static final int MAXIMUM_MESSAGE_SIZE = 134;
  private static final int MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE = 134;

  private static String latinAlphabetPattern;
  private static AlphabetEncoding latinEncoding;
  private static Integer latinDataCoding;

  private static AlphabetEncoding nonLatinEncoding;
  private static Integer nonLatinDataCoding;

  public static void initialize(final Properties properties) throws Exception {
    latinAlphabetPattern = properties.getProperty("smssender.latin_message.pattern");
    latinEncoding = getEncoding(properties.getProperty("smssender.latin_message.encoding"));

    if (properties.getProperty("smssender.latin_message.data_coding") != null)
      latinDataCoding = Integer.parseInt(properties.getProperty("smssender.latin_message.data_coding"));
    else
      latinDataCoding = null;

    nonLatinEncoding = getEncoding(properties.getProperty("smssender.non_latin_message.encoding"));

    if (properties.getProperty("smssender.non_latin_message.data_coding") != null)
      nonLatinDataCoding = Integer.parseInt(properties.getProperty("smssender.non_latin_message.data_coding"));
    else
      nonLatinDataCoding = null;
  }

  public static byte[][] splitUnicodeMessage(final byte[] aMessage) {
    final byte UDHIE_HEADER_LENGTH = 0x05;
    final byte UDHIE_IDENTIFIER_SAR = 0x00;
    final byte UDHIE_SAR_LENGTH = 0x03;

    // determine how many messages have to be sent
    int numberOfSegments = aMessage.length / MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE;
    int messageLength = aMessage.length;
    if (numberOfSegments > 255) {
      numberOfSegments = 255;
      messageLength = numberOfSegments * MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE;
    }
    if ((messageLength % MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE) > 0) {
      numberOfSegments++;
    }

    // prepare array for all of the msg segments
    byte[][] segments = new byte[numberOfSegments][];

    int lengthOfData;

    // generate new reference number
    byte[] referenceNumber = new byte[1];
    new Random().nextBytes(referenceNumber);

    // split the message adding required headers
    for (int i = 0; i < numberOfSegments; i++) {
      if (numberOfSegments - i == 1) {
        lengthOfData = messageLength - i * MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE;
      } else {
        lengthOfData = MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE;
      }
      // new array to store the header
      segments[i] = new byte[6 + lengthOfData];

      // UDH header
      // doesn't include itself, its header length
      segments[i][0] = UDHIE_HEADER_LENGTH;
      // SAR identifier
      segments[i][1] = UDHIE_IDENTIFIER_SAR;
      // SAR length
      segments[i][2] = UDHIE_SAR_LENGTH;
      // reference number (same for all messages)
      segments[i][3] = referenceNumber[0];
      // total number of segments
      segments[i][4] = (byte) numberOfSegments;
      // segment number
      segments[i][5] = (byte) (i + 1);
      // copy the data into the array
      System.arraycopy(aMessage, (i * MAXIMUM_MULTIPART_MESSAGE_SEGMENT_SIZE), segments[i], 6, lengthOfData);
    }
    return segments;
  }

  public static AlphabetEncoding getEncoding(final String message, Ref<Integer> dataCoding, Ref<Boolean> isLatinDataCoding) {
    final AlphabetEncoding encoding;
    if (message.matches(latinAlphabetPattern)) {
      isLatinDataCoding.setValue(true);
      encoding = latinEncoding;

      if (latinDataCoding != null)
        dataCoding.setValue(latinDataCoding);
      else
        dataCoding.setValue(encoding.getDataCoding());
    } else {
      isLatinDataCoding.setValue(false);
      encoding = nonLatinEncoding;

      if (nonLatinDataCoding != null)
        dataCoding.setValue(nonLatinDataCoding);
      else
        dataCoding.setValue(encoding.getDataCoding());
    }

    return encoding;
  }

  public static String decodeLatinMessage(byte[] message) {
    return latinEncoding.decode(message);
  }

  public static String decodeNonLatinMessage(byte[] message) {
    return nonLatinEncoding.decode(message);
  }

  private static AlphabetEncoding getEncoding(final String name) throws Exception {
    if (name.equalsIgnoreCase("default")) {
      return new DefaultAlphabetEncoding();
    }

    if (name.equalsIgnoreCase("ascii")) {
      return new ASCIIEncoding();
    }

    if (name.equalsIgnoreCase("ucs2")) {
      return new UCS2Encoding();
    }

    if (name.equalsIgnoreCase("hpRoman8")) {
      return new HPRoman8Encoding();
    }

    if (name.equalsIgnoreCase("Latin1")) {
      return new Latin1Encoding();
    }

    if (name.equalsIgnoreCase("utf16-le")) {
      return new UTF16Encoding(false);
    }

    if (name.equalsIgnoreCase("utf16-be")) {
      return new UTF16Encoding(true);
    }

    throw new Exception(String.format("Unknown encoding '%s'", name));
  }
}