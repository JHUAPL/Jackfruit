package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The ParserClass annotation specifies a class which implements the {@link Parser} interface to
 * convert an object to and from a String.
 * 
 * @author nairah1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ParserClass {
  public Class<?> value();
}
