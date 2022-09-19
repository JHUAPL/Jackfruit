package srncfg.annotations;

public interface Parser<T> {
  public T fromString(String s);

  public String toString(T t);
}
