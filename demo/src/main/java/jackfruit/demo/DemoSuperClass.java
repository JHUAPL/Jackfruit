package jackfruit.demo;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.ParserClass;

// prefix can be superseded by inherited classes
@Jackfruit(prefix = "super ")
public abstract class DemoSuperClass extends DemoSuperSuperClass {

  @Comment("from DemoSuperClass")
  @DefaultValue("-1")
  @Override
  public abstract int inherited();

  @Comment("from DemoSuperClass")
  @DefaultValue("2")
  @Override
  public abstract int intMethod();
  
  @Comment("This string is serialized into an object\n\tThis comment contains a newline character, and this line starts with a tab.")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public abstract SomeRandomClass randomClass();
}
