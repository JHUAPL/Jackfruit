package jackfruit.demo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class JackfruitDemo {

  public static void main(String[] args) {

    // this factory is built by the annotation processor after reading
    // DemoClass
    DemoClassFactory factory = new DemoClassFactory();

    // get an example config object
    DemoClass template = factory.getTemplate();

    /*-
    // or if you prefer, use an interface
    DemoInterfaceFactory factory = new DemoInterfaceFactory();
    DemoInterface template = factory.getTemplate();
    */

    // generate an Apache PropertiesConfiguration object. This can be written out to
    // a file
    PropertiesConfiguration config =
        factory.toConfig(template, new PropertiesConfigurationLayout());

    try {
      // this is the template, with comments
      System.out.println("*** This is the template, with default values ***");
      config.write(new PrintWriter(System.out));

      // write to file
      File tmpFile = File.createTempFile("tmp", ".config");
      tmpFile.deleteOnExit();
      System.out.println("wrote " + tmpFile.getAbsolutePath());
      config.write(new PrintWriter(tmpFile));

      System.out.println("\n*** This is the configuration read from " + tmpFile.getAbsolutePath());
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
    System.out.println("\n*** Retrieving a configuration value");
    List<SomeRandomClass> randoms = template.randoms();
    for (SomeRandomClass random : randoms)
      System.out.println("random.toUpperCase() = " + random.toUpperCase());

    // create a new factory with a different prefix, but same parameters
    factory = new DemoClassFactory("anotherPrefix");
    template = factory.fromConfig(config);
    
    // this will not find anything
    randoms = template.randoms();
    for (SomeRandomClass random : randoms)
      System.out.println("random.toUpperCase() = " + random.toUpperCase());
  }

}
