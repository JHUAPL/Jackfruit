package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Comment annotation specifies the comment that appears in the configuration file above the
 * parameter itself.
 * 
 * @author nairah1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Comment {
  public String value() default "";
}
