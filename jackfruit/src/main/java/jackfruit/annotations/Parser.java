package jackfruit.annotations;

/**
 * Interface to serialize/deserialize a string to a custom class.
 * 
 * @author nairah1
 *
 * @param <T>
 */
public interface Parser<T> {
  public T fromString(String s);

  public String toString(T t);
}
