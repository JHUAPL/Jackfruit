package jackfruit.processor;

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

import jackfruit.annotations.Parser;
import java.util.List;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;

/**
 * Holds annotation information and other metadata about an annotated method.
 *
 * @author Hari.Nair@jhuapl.edu
 */
@Value.Immutable
public abstract class AnnotationBundle {

  /**
   * @return the return type of this method without any parameters (e.g. return List rather than
   *     List&lt;String&gt;)
   */
  public abstract TypeMirror erasure();

  /**
   * @return the parameterized types, if any of this method (e.g. return String if the annotated
   *     method returns List&lt;String&gt;)
   */
  public abstract List<TypeMirror> typeArgs();

  /**
   * @return Comment for this configuration parameter. This can be blank.
   */
  public abstract String comment();

  /**
   * @return Default value for this configuration parameter. This is required.
   */
  public abstract String defaultValue();

  /**
   * @return Key used in the configuration file. If omitted, default value is the name of the
   *     configuration parameter (the method name).
   */
  public abstract String key();

  /**
   * @return If this configuration parameter is not a string or primitive/boxed type, this class
   *     will convert the string to the proper object and vice versa. This class must implement
   *     {@link jackfruit.annotations.Parser}.
   */
  public abstract Optional<TypeMirror> parserClass();
}
