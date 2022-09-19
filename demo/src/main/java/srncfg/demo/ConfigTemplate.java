package srncfg.demo;

import srncfg.annotations.Comment;
import srncfg.annotations.ConfigParams;
import srncfg.annotations.DefaultValue;
import srncfg.annotations.Key;
import srncfg.annotations.ParserClass;

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
 * <li>
 * </ul>
 * <p>
 * How to handle blocks like List<MISEObservingMode>?
 * <p>
 * Use <a href="https://github.com/square/javapoet">JavaPoet</a> to build a factory?
 * <p>
 * Inspired by <a href="http://owner.aeonbits.org/">owner</a>.
 * 
 * @author nairah1
 *
 */

@ConfigParams(prefix = "prefix")
public interface ConfigTemplate {

  // default key is field name
  @Key("key")
  @Comment("field comment")
  @DefaultValue("0")
  public int intMethod();

  @DefaultValue("0.")
  public Double doubleMethod();

  @DefaultValue("Default String")
  public String StringMethod();

  // parser creates an object from a string
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public SomeRandomClass randomClass();

}
