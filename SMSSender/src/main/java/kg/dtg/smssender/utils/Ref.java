package kg.dtg.smssender.utils;

public class Ref<T> {
  private T value;

  public Ref() { }

  public Ref(T value) {
    this.value = value;
  }

  public final T getValue() {
    return value;
  }

  public final void setValue(T value) {
    this.value = value;
  }
}
