package jackfruit.demo;

import java.io.IOException;
import java.io.PrintWriter;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class JackfruitDemo {

  public static void main(String[] args) {

    // this factory is built by the annotation processor after reading the DemoConfig interface
    DemoConfigFactory factory = new DemoConfigFactory();

    // get an example config object
    DemoConfig template = factory.getTemplate();

    // generate an Apache PropertiesConfiguration object.  This can be written out to a file
    PropertiesConfiguration config = factory.toConfig(template);

    try {
      config.write(new PrintWriter(System.out));
    } catch (ConfigurationException | IOException e) {
      e.printStackTrace();
    }

    // create a config object from an Apache PropertiesConfiguration
    template = factory.fromConfig(config);
    
    System.out.printf("config.StringMethod() = %s\n", template.StringMethod());
    
    

  }

}
