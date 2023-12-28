package jackfruit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Include annotation includes another class annotated with @Jackfruit.  Example:
 * <p>
 * <code>
 * &#64;Include
 * AnotherBlockType anotherBlockType();
 * </code>
 * </p>
 * This allows for a configuration type to access other configuration types
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Include {
}
