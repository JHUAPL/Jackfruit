package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to signify that it should be run through the annotation processor. There is
 * an optional "prefix" argument that can be used to add a prefix to all of the configuration keys
 * created by the processor.
 * <p>
 * For example: <br>
 * &#x0040;Jackfruit(prefix = "myPrefix")
 * <p>
 * Inspired by <a href="http://owner.aeonbits.org/">owner</a>.
 * 
 * @author nairah1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Jackfruit {
  public String prefix() default "";
}
