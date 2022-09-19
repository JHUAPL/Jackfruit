package jackfruit.demo;

import jackfruit.annotations.Parser;

public class SomeRandomClassParser implements Parser<SomeRandomClass> {

  @Override
  public SomeRandomClass fromString(String s) {
    return new SomeRandomClass(s);
  }

  @Override
  public String toString(SomeRandomClass src) {
    return src.getInternalString();
  }


}
