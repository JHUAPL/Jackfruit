package jackfruit.demo;

/*-
 * #%L
 * jackfruit-demo
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Laboratory
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.Key;
import jackfruit.annotations.ParserClass;
import java.util.List;

/**
 * &#x0040;Jackfruit on interface
 *
 * <ul>
 *   <li>prefix is optional
 * </ul>
 *
 * Method annotations:
 *
 * <ul>
 *   <li>&#x0040;Key
 *       <ul>
 *         <li>If omitted value is method name
 *       </ul>
 *   <li>&#x0040;Comment
 *       <ul>
 *         <li>Optional
 *       </ul>
 *   <li>&#x0040;DefaultValue
 *       <ul>
 *         <li>Required, String value
 *       </ul>
 *   <li>&#x0040;Parser
 *       <ul>
 *         <li>Optional, name of class to create object from String using its fromString() method
 *       </ul>
 *       <p>Inspired by <a href="http://owner.aeonbits.org/">owner</a>.
 * </ul>
 *
 * @author Hari.Nair@jhuapl.edu
 */
@Jackfruit(prefix = "prefix")
public interface DemoInterface {

  // default key is field name
  @Key("key")
  @Comment("One line comment")
  @DefaultValue("1")
  int intMethod();

  @Comment(
      "This is a very long comment line that really should be wrapped into more than one line but that's really up to you.")
  @DefaultValue("0.")
  Double doubleMethod();

  @Comment(
      """
      This is a multiline
      java text block.

      This is a new paragraph.
      """)
  @DefaultValue("Default String")
  String StringMethod();

  @Comment(
      "This string is serialized into an object\n\tThis comment contains a newline character, and this line starts with a tab.")
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
