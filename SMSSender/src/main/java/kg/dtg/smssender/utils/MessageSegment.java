package kg.dtg.smssender.utils;

public final class MessageSegment {
  private byte[] data;

  private int startIndex;

  private int length;

  public final byte[] getData() {
    return data;
  }

  public final void setData(byte[] data) {
    this.data = data;
  }

  public final int getStartIndex() {
    return startIndex;
  }

  public final void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public final int getLength() {
    return length;
  }

  public final void setLength(int length) {
    this.length = length;
  }
}
