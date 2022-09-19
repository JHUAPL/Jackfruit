# jackfruit

## Introduction

Jackfruit processes annotations on Java interfaces to generate code that can read and write Apache Configuration files.  Include Jackfruit in your project with the following POM:

```
POM GOES HERE
```

An example of an annotated interface is
```
@Jackfruit(prefix = "prefix")
public interface DemoConfig {

  // default key is field name
  @Key("key")
  @Comment("field comment")
  @DefaultValue("0")
  public int intMethod();

  @DefaultValue("0.")
  public Double doubleMethod();

  @DefaultValue("Default String")
  public String StringMethod();

  @Comment("This string is serialized into an object")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public SomeRandomClass randomClass();
}
```

This corresponds to this Apache Configuration file:
```
# field comment
prefix.key = 0
prefix.doubleMethod = 0.0
prefix.StringMethod = Default String
# This string is serialized into an object
prefix.randomClass = serialized string
```

The annotation processor generates a class called DemoConfigFactory.  An example of use is
```
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

      // print to screen 
      config.write(new PrintWriter(System.out));

    } catch (ConfigurationException | IOException e) {
      e.printStackTrace();
    }

    // create a config object from an Apache PropertiesConfiguration
    template = factory.fromConfig(config);

    // get one of the config object's properties
    System.out.printf("config.StringMethod() = %s\n", template.StringMethod());
```