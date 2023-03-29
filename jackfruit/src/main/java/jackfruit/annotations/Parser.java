package jackfruit.annotations;

/**
 * Interface to serialize/deserialize a string to a custom class.
 * 
 * @author nairah1
 *
 * @param <T>
 */
public interface Parser<T> {
  T fromString(String s);

  String toString(T t);
}
