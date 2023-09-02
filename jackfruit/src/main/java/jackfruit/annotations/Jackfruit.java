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
 * Use this annotation to signify that it should be run through the annotation processor. There is
 * an optional "prefix" argument that can be used to add a prefix to all the configuration keys
 * created by the processor.
 *
 * <p>For example: <br>
 * &#x0040;Jackfruit(prefix = "myPrefix")
 *
 * <p>Inspired by <a href="http://owner.aeonbits.org/">owner</a>.
 *
 * @author Hari.Nair@jhuapl.edu
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Jackfruit {
  String prefix() default "";
}
