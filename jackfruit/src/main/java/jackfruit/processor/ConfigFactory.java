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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;

/**
 * This interface converts instances of annotated interfaces of type T to Apache Commons
 * Configuration files and vice versa.
 *
 * @author Hari.Nair@jhuapl.edu
 * @param <T> configuration class with annotations
 */
public interface ConfigFactory<T> {

  /**
   * @return This returns an object of type T with default values.
   */
  T getTemplate();

  /**
   * @param config configuration to turn into an object of type T
   * @return an object of type T from the supplied Apache Commons {@link Configuration}.
   */
  T fromConfig(Configuration config);

  /**
   * @param t object to convert to a configuration
   * @param layout used for formatting the returned PropertiesConfiguration
   * @return an Apache Commons {@link PropertiesConfiguration} from the supplied object T.
   */
  PropertiesConfiguration toConfig(T t, PropertiesConfigurationLayout layout);

  /**
   * This is simply a call to {@link #toConfig(Object, PropertiesConfigurationLayout)} with a new
   * PropertiesConfigurationLayout().
   *
   * @param t object to convert to a configuration
   * @return an Apache Commons {@link PropertiesConfiguration} from the supplied object T.
   */
  default PropertiesConfiguration toConfig(T t) {
    return toConfig(t, new PropertiesConfigurationLayout());
  }
}
