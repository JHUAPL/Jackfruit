# jackfruit

## Quick start

Jackfruit processes annotations on Java interfaces to generate code that can read and write Apache Configuration files.  The `demo` module includes sample code.  In the top level directory, run `mvn clean package`, which will build the annotation library, run the annotation processor on the file `demo/src/main/java/jackfruit/demo/DemoConfig.java`, and generate the class `demo/target/generated-sources/annotations/jackfruit/demo/DemoConfigFactory.java`.  The file `demo/src/main/java/jackfruit/demo/JackfruitDemo.java` shows some simple examples of use.

## Introduction

Include Jackfruit in your project with the following POM:

```
<dependency>
    <groupId>edu.jhuapl.ses.srn</groupId>
    <artifactId>jackfruit</artifactId>
    <version>$VERSION</version>
    <type>pom</type>
</dependency>
```

Find the latest version at [Surfshop](http://surfshop:8082/ui/repos/tree/General/libs-snapshot-local/edu/jhuapl/ses/srn/jackfruit/).

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

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  List<Double> doubles();
  
  @Comment("List of RandomClass")
  @DefaultValue("obj1 obj2")
  @ParserClass(SomeRandomClassParser.class)
  List<SomeRandomClass> randoms();
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
# List of Doubles
prefix.doubles = 0.0
prefix.doubles = 5.34
prefix.doubles = 17.0
# List of RandomClass
prefix.randoms = obj1
prefix.randoms = obj2
```

The annotation processor generates a class called DemoConfigFactory.  An example of use is
```
    // this factory is built by the annotation processor after reading the DemoConfig interface
    DemoConfigFactory factory = new DemoConfigFactory();

    // get an example config object
    DemoConfig template = factory.getTemplate();

    // generate an Apache PropertiesConfiguration object. This can be written out to a file
    PropertiesConfiguration config = factory.toConfig(template);

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

## Supported Annotations

The `@Jackfruit` annotation goes on the interface.  The remaining annotations are for use on the interface methods.

### Jackfruit
This annotation goes on the interface to signify that it should be run through the annotation processor.  There is an optional "prefix" argument that can be used to add a prefix to all of the configuration keys created by the processor.

### Comment

The `@Comment` annotation specifies the comment that appears in the configuration file above the parameter itself.

### DefaultValue

The `@DefaultValue` annotation is a String used to initialize the parameter.  This is a required annotation.  Strings and primitives (and their corresponding wrapper types) are read natively.  Other objects will need to use the `@ParserClass` annotation to specify a class which implements the `jackfruit.annotations.Parser` interface to convert the object to and from a String.

### Key

The `@Key` annotation can be used to specify the name for the configuration key.  The default is to use the name of the method.

### ParserClass

The `@ParserClass` annotation specifies a class which implements the `jackfruit.annotations.Parser` interface to convert an object to and from a String.  The `Parser` interface implements two methods:

```
public interface Parser<T> {
  public T fromString(String s);

  public String toString(T t);
}
```
