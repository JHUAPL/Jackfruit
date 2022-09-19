package jackfruit.processor;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;

public interface ConfigFactory<T> {

  public T getTemplate();
  
  public T fromConfig(Configuration config);

  public PropertiesConfiguration toConfig(T t);

}
