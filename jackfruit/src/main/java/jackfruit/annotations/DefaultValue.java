package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The DefaultValue annotation is a String used to initialize the parameter. This is a required
 * annotation. Methods without this annotation are ignored.
 * <p>
 * Strings and primitives (and their corresponding wrapper types) are read natively. Other objects
 * will need to use the {@link ParserClass} annotation to specify a class which implements the
 * {@link Parser} interface to convert the object to and from a String.
 * 
 * @author nairah1
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface DefaultValue {
  public String value() default "";
}
