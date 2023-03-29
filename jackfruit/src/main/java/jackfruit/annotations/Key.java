package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Key annotation can be used to specify the name for the configuration key. The default is to
 * use the name of the method.
 * 
 * @author nairah1
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Key {
  String value() default "";
}
