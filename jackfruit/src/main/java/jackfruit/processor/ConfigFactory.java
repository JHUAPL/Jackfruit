package jackfruit.processor;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;

/**
 * This interface converts instances of annotated interfaces of type T to Apache Commons
 * Configuration files and vice versa.
 * 
 * @author nairah1
 *
 * @param <T>
 */
public interface ConfigFactory<T> {

  /**
   * This returns an object of type T with default values.
   * 
   * @return
   */
  T getTemplate();

  /**
   * This creates an object of type T from the supplied Apache Commons {@link Configuration}.
   * 
   * @param config
   * @return
   */
  T fromConfig(Configuration config);

  /**
   * This creates an Apache Commons {@link PropertiesConfiguration} from the supplied object T.
   * 
   * @param t
   * @param layout used for formatting the returned PropertiesConfiguration
   * @return
   */
  PropertiesConfiguration toConfig(T t, PropertiesConfigurationLayout layout);

  /**
   * This creates an Apache Commons {@link PropertiesConfiguration} from the supplied object T. This
   * is simply a call to {@link #toConfig(Object, PropertiesConfigurationLayout)} with a new
   * PropertiesConfigurationLayout().
   * 
   * @param t
   * @return
   */
  default PropertiesConfiguration toConfig(T t) {
    return toConfig(t, new PropertiesConfigurationLayout());
  }

}
