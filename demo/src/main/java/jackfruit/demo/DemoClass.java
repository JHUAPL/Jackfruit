package jackfruit.demo;

import java.util.List;
import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.ParserClass;

@Jackfruit(prefix = "prefix")
public abstract class DemoClass extends DemoSuperClass {

  @Comment("This method's key name is inherited from DemoSuperSuperClass")
  @DefaultValue("1")
  @Override
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

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  public abstract List<Double> doubles();

  @DefaultValue("set in DemoClass, parser inherited from DemoSuperClass")
  @Override
  public abstract SomeRandomClass randomClass();

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
