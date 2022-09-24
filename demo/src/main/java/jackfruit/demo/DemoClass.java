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
  @Comment("One line comment")
  @DefaultValue("1")
  public abstract int intMethod();

  @Comment("This is a very long comment line that really should be wrapped into more than one line but that's really up to you.")
  @DefaultValue("0.")
  public abstract Double doubleMethod();

  @Comment("""
      This is a multiline
      java text block.

      This is a new paragraph.
      """)
  @DefaultValue("Default String")
  public abstract String StringMethod();

  @Comment("This string is serialized into an object\n\tThis comment contains a newline character, and this line starts with a tab.")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public abstract SomeRandomClass randomClass();

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  public abstract List<Double> doubles();

  @Comment("List of RandomClass")
  @DefaultValue("""
      obj1
      obj2

      obj3 obj4
      """)
  @ParserClass(SomeRandomClassParser.class)
  public abstract List<SomeRandomClass> randoms();

  public void noAnnotationsOnThisMethod() {
    System.out.println("This method was not processed since it has no DefaultValue annotation");
  }

}
