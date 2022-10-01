package jackfruit.demo;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;

@Jackfruit
public abstract class DemoSuperSuperClass {

  @Comment("from DemoSuperSuperClass")
  @DefaultValue("-3")
  public abstract int inherited2();
  
  @Comment("from DemoSuperSuperClass")
  @DefaultValue("-2")
  public abstract int inherited();

  @Comment("from DemoSuperSuperClass")
  @DefaultValue("3")
  public abstract int intMethod();
}
