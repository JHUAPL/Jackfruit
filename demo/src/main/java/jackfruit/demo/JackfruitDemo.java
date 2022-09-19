package jackfruit.demo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class JackfruitDemo {

  public static void main(String[] args) {

    // this factory is built by the annotation processor after reading the DemoConfig interface
    DemoConfigFactory factory = new DemoConfigFactory();

    // get an example config object
    DemoConfig template = factory.getTemplate();

    // generate an Apache PropertiesConfiguration object. This can be written out to a file
    PropertiesConfiguration config =
        factory.toConfig(template, new PropertiesConfigurationLayout());

    try {
      // this is the template, with comments
      config.write(new PrintWriter(System.out));

      // write to file
      File tmpFile = File.createTempFile("tmp", ".config");
      tmpFile.deleteOnExit();
      // System.out.println("wrote "+tmpFile.getAbsolutePath());
      config.write(new PrintWriter(tmpFile));

      // read from file
      config = new Configurations().properties(tmpFile);

      // print to screen - this still has comments?
      config.write(new PrintWriter(System.out));

    } catch (ConfigurationException | IOException e) {
      e.printStackTrace();
    }

    // create a config object from an Apache PropertiesConfiguration
    template = factory.fromConfig(config);

    // get one of the config object's properties
    System.out.printf("config.StringMethod() = %s\n", template.StringMethod());
  }

}
