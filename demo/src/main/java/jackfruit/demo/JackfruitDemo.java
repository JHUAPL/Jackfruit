package jackfruit.demo;

/*-
 * #%L
 * jackfruit-demo
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

      // add 999 to the default double values
      List<Double> doubles = template.doubles();
      doubles.add(999.);
      config = factory.withDoubles(config, doubles);

      // write to file
      File tmpFile = File.createTempFile("tmp", ".config");
      tmpFile.deleteOnExit();
      System.out.println("wrote " + tmpFile.getAbsolutePath());
      config.write(new PrintWriter(tmpFile));

      System.out.println("\n*** This is the configuration read from " + tmpFile.getAbsolutePath());
      System.out.println("*** Note 999 has been added to doubles");
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
    System.out.println();
    System.out.println(
        "Create a config with anotherPrefix and retrieve randoms.  This will throw an exception.");
    factory = new DemoClassFactory("anotherPrefix");
    template = factory.fromConfig(config);

    // this will throw an exception since anotherPrefix.randoms is not one of the keys in the
    // configuration.
    try {
      randoms = template.randoms();
    } catch (Exception e) {
      System.out.println("Caught expected RuntimeException:\n" + e.getLocalizedMessage());
      randoms = new ArrayList<>();
    }
    for (SomeRandomClass random : randoms)
      System.out.println("random.toUpperCase() = " + random.toUpperCase());
  }
}
