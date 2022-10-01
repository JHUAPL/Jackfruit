package jackfruit.demo;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;

// prefix can be superseded by inherited classes
@Jackfruit
public abstract class DemoSuperClass extends DemoSuperSuperClass {

  @Comment("from DemoSuperClass")
  @DefaultValue("-1")
  @Override
  public abstract int inherited();

  @Comment("from DemoSuperClass")
  @DefaultValue("2")
  @Override
  public abstract int intMethod();
}
