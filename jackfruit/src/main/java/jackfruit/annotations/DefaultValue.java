package jackfruit.annotations;

/*-
 * #%L
 * jackfruit
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Lab
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
  String value() default "";
}
