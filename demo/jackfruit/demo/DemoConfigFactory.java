package jackfruit.demo;

import jackfruit.processor.ConfigFactory;
import java.lang.Double;
import java.lang.Override;
import java.lang.String;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DemoConfigFactory implements ConfigFactory<DemoConfig> {
  private static final Logger logger = LogManager.getLogger();

  @Override
  public PropertiesConfiguration toConfig(DemoConfig t) {
    PropertiesConfiguration config = new PropertiesConfiguration();
    config.setProperty("prefix.key", t.intMethod());
    config.setProperty("prefix.doubleMethod", t.doubleMethod());
    config.setProperty("prefix.StringMethod", t.StringMethod());
    SomeRandomClassParser randomClassparser = new SomeRandomClassParser();
    config.setProperty("prefix.randomClass", randomClassparser.toString(t.randomClass()));
    return config;
  }

  @Override
  public DemoConfig fromConfig(Configuration config) {
    return new DemoConfig() {
      /**
       * field comment
       */
      public int intMethod() {
        return config.getInt("prefix.key");
      }

      public Double doubleMethod() {
        return config.getDouble("prefix.doubleMethod");
      }

      public String StringMethod() {
        return config.getString("prefix.StringMethod");
      }

      public SomeRandomClass randomClass() {
        SomeRandomClassParser parser = new SomeRandomClassParser();
        return parser.fromString(config.getString("prefix.randomClass"));
      }
    };
  }

  @Override
  public DemoConfig getTemplate() {
    return new DemoConfig() {
      /**
       * field comment
       */
      public int intMethod() {
        return 0;
      }

      public Double doubleMethod() {
        return 0.;
      }

      public String StringMethod() {
        return "Default String";
      }

      public SomeRandomClass randomClass() {
        SomeRandomClassParser parser = new SomeRandomClassParser();
        return parser.fromString("serialized string");
      }
    };
  }
}
