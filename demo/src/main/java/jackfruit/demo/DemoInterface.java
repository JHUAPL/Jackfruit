package jackfruit.demo;

import java.util.List;
import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.Key;
import jackfruit.annotations.ParserClass;

/**
 * &#x0040;Configurable on interface
 * <ul>
 * <li>prefix is optional</li>
 * </ul>
 * Method annotations:
 * <ul>
 * <li>&#x0040;Key
 * <ul>
 * <li>If omitted value is method name</li>
 * </ul>
 * </li>
 * <li>&#x0040;Comment
 * <ul>
 * <li>Optional</li>
 * </ul>
 * </li>
 * <li>&#x0040;DefaultValue
 * <ul>
 * <li>Required, String value</li>
 * </ul>
 * </li>
 * <li>&#x0040;Parser
 * <ul>
 * <li>Optional, name of class to create object from String using its fromString() method</li>
 * </ul>
 * <p>
 * Inspired by <a href="http://owner.aeonbits.org/">owner</a>.
 * 
 * @author nairah1
 *
 */

@Jackfruit(prefix = "prefix")
public interface DemoInterface {

  // default key is field name
  @Key("key")
  @Comment("field comment")
  @DefaultValue("0")
  int intMethod();

  @DefaultValue("0.")
  Double doubleMethod();

  @DefaultValue("Default String")
  String StringMethod();

  @Comment("This string is serialized into an object")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  SomeRandomClass randomClass();

  @Comment("List of Doubles")
  @DefaultValue("0. 5.34 17")
  List<Double> doubles();
  
  @Comment("List of RandomClass")
  @DefaultValue("obj1 obj2")
  @ParserClass(SomeRandomClassParser.class)
  List<SomeRandomClass> randoms();
  
}