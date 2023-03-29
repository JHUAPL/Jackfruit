package jackfruit.processor;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;

/**
 * This interface converts instances of annotated interfaces of type T to Apache Commons
 * Configuration files and vice versa.
 *
 * @author nairah1
 * @param <T>
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
