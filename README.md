# Jackfruit

## Quick start

Jackfruit processes annotations on Java interfaces and abstract classes to generate code that can read and write Apache Configuration files.  In the top level directory, run `mvn clean package`, which will build the annotation library in the `jackfruit` module and run the annotation processor in the `demo` module.  The file `demo/src/main/java/jackfruit/demo/JackfruitDemo.java` shows some simple examples of use.

## Introduction

Include Jackfruit in your project with the following POM:

```
<dependency>
    <groupId>edu.jhuapl.ses</groupId>
    <artifactId>jackfruit</artifactId>
    <version>$VERSION</version>
    <type>pom</type>
</dependency>
```

Find the latest version at [Maven Central](https://central.sonatype.com/artifact/edu.jhuapl.ses/jackfruit).

The annotation processor runs on any interface or abstract class annotated with `@Jackfruit`
```
@Jackfruit(prefix = "prefix")
public interface DemoInterface {

  // default key is field name
  @Key("key")
  @Comment("One line comment")
  @DefaultValue("1")
  int intMethod();

  @Comment("This is a very long comment line that really should be wrapped into more than one line but that's really up to you.")
  @DefaultValue("0.")
  Double doubleMethod();

  @Comment("""
      This is a multiline
      java text block.

      This is a new paragraph.
      """)
  @DefaultValue("Default String")
  String StringMethod();

  @Comment("This string is serialized into an object\n\tThis comment contains a newline character, and this line starts with a tab.")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  SomeRandomClass randomClass();

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  List<Double> doubles();

  @Comment("List of RandomClass")
  @DefaultValue("""
          obj1
          obj2
          
          obj3 obj4
          """)
  @ParserClass(SomeRandomClassParser.class)
  List<SomeRandomClass> randoms();

}
```

This corresponds to the following Apache Configuration file:
```
# One line comment
prefix.key = 1
# This is a very long comment line that really should be wrapped into more than one line but that's really up to you.
prefix.doubleMethod = 0.0
# This is a multiline
# java text block.

# This is a new paragraph.

prefix.StringMethod = Default String
# This string is serialized into an object
# 	This comment contains a newline character, and this line starts with a tab.
prefix.randomClass = serialized string
# List of Doubles
prefix.doubles = 0.0
prefix.doubles = 5.34
prefix.doubles = 17.0
# List of RandomClass
prefix.randoms = obj1
prefix.randoms = obj2
prefix.randoms = obj3
prefix.randoms = obj4
```

The annotation processor generates a class called DemoInterfaceFactory.  An example of use is
```
    // this factory is built by the annotation processor after reading the DemoConfig interface
    DemoInterfaceFactory factory = new DemoInterfaceFactory();

    // get an example config object
    DemoInterface template = factory.getTemplate();

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
    System.out.println("\n*** Retrieving a configuration value");
    List<SomeRandomClass> randoms = template.randoms();
    for (SomeRandomClass random : randoms)
      System.out.println("random.toUpperCase() = " + random.toUpperCase());
```

## Inheritance

Jackfruit annotations can be inherited by derived classes.  The `@Jackfruit` annotation must be present on the parent class as well as the inherited class.  The annotation processor will build factory classes for both parent and child classes.

## Supported Annotations

The `@Jackfruit` annotation goes on the abstract type.  The remaining annotations are for use on methods.  Jackfruit will only process methods annotated either with `@DefaultValue` or `@Include`.

### Jackfruit
This annotation goes on the abstract type to signify that it should be run through the annotation processor.  There is an optional "prefix" argument that can be used to add a prefix to all of the configuration keys created by the processor.  The Jackfruit annotation is not inherited by derived classes.

Every Factory class has a default constructor with this prefix (or an empty string if no prefix is specified) and a constructor where a prefix may be supplied.  This is useful in case the user wants to create many configurations with the same parameters but different prefixes.  

To use the same set of parameters with a different prefix:
```
    DemoInterfaceFactory factory = new DemoInterfaceFactory("anotherPrefix");
```

### Comment

The `@Comment` annotation specifies the comment that appears in the configuration file above the parameter itself.  This can be a multiline text block and include newlines and tabs.

### DefaultValue

The `@DefaultValue` annotation is a String used to initialize the parameter.  Strings and primitives (and their corresponding wrapper types) are read natively.  Other objects will need to use the `@ParserClass` annotation to specify a class which implements the `jackfruit.annotations.Parser` interface to convert the object to and from a String.  This annotation must be present if `@Include` is not used.

### Include

The `@Include` annotation allows the user to include another configuration class within this one.  For example, if ThisBlock.java contains

```
@Jackfruit(prefix = "thisBlock")
public interface ThisBlock {
  @Comment("thisBlock")
  @DefaultValue("1")
  int intMethod();
  
@Include
OtherBlock otherBlock();
}
```
and OtherBlock.java contains
```
@Jackfruit(prefix = "otherBlock")
public interface OtherBlock {
  @Comment("OtherBlock")
  @DefaultValue("2")
  int intMethod();
}

```
running new ThisBlockFactory().getTemplate() will create
```
# thisBlock
thisBlock.intMethod = 1

# OtherBlock
otherBlock.intMethod = 2
```

If `@Include` is present, no other annotations will be honored.

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

## Modifying a configuration

If you'd like to create a configuration object that differs from the template populated with default values or one you've loaded from a file, the factory has "with" methods that will return a new PropertiesConfiguration object with a value replaced.  For example:

```
    DemoInterfaceFactory factory = new DemoInterfaceFactory();
    DemoInterface template = factory.getTemplate();
    PropertiesConfiguration config = factory.toConfig(template);
    // add 999 to the default double values
    List<Double> doubles = template.doubles();
    doubles.add(999.);
    config = factory.withDoubles(config, doubles);
    config = factory.withIntMethod(config, 4);
```

This replaces the "doubles" and the "key" properties.  The "with" methods are just wrappers around the "setProperty" method of the PropertiesConfiguration class.  You can now use `fromConfig()` to create a new DemoInterface object with the updated values.


