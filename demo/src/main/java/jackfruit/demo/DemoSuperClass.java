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

  @Comment(
      "This string is serialized into an object\n\tThis comment contains a newline character, and this line starts with a tab.")
  @DefaultValue("serialized string")
  @ParserClass(SomeRandomClassParser.class)
  public abstract SomeRandomClass randomClass();
}
