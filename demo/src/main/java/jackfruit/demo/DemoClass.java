package jackfruit.demo;

import java.util.List;
import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.Key;
import jackfruit.annotations.ParserClass;

@Jackfruit(prefix = "prefix")
public abstract class DemoClass {

  @Key("key")
  @Comment("field comment")
  @DefaultValue("0")
  public abstract int intMethod();

  @DefaultValue("0.")
  public abstract Double doubleMethod();

  @DefaultValue("Default String")
  public abstract String StringMethod();

  @Comment("This string is serialized into an object")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public abstract SomeRandomClass randomClass();

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  public abstract List<Double> doubles();

  @Comment("List of RandomClass")
  @DefaultValue("obj1 obj2")
  @ParserClass(SomeRandomClassParser.class)
  public abstract List<SomeRandomClass> randoms();

  public void noAnnotationsOnThisMethod() {
    System.out.println("This method was not processed");
  }
  
}
