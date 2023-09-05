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

import crucible.crust.logging.Log4j2Configurator;
import jackfruit.processor.ConfigProcessor;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.Test;

/**
 * From <a href=
 * "https://stackoverflow.com/questions/21427301/debugging-annotation-processors-in-eclipse">https://stackoverflow.com/questions/21427301/debugging-annotation-processors-in-eclipse</a>
 *
 * @author Hari.Nair@jhuapl.edu
 */
public class TestProcessor {



  @Test
  public void runProcessor() throws Exception {

    /*-
    System.out.println("TestProcessor");
    System.out.println("java.class.path: ");
    System.out.println(System.getProperty("java.class.path"));
    */

    boolean ignore = true;
    if (!ignore) {
      Log4j2Configurator lc = Log4j2Configurator.getInstance();
      lc.setPattern("%highlight{%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%c{1}:%L] %msg%n%throwable}");

      String source = System.getProperty("user.dir");
      Iterable<JavaFileObject> files = getSourceFiles(source);

      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

      CompilationTask task =
          compiler.getTask(new PrintWriter(System.out), null, null, null, null, files);
      task.setProcessors(List.of(new ConfigProcessor()));

      task.call();
    }
  }

  private Iterable<JavaFileObject> getSourceFiles(String p_path) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null);

    files.setLocation(StandardLocation.SOURCE_PATH, List.of(new File(p_path)));

    Set<Kind> fileKinds = Collections.singleton(Kind.SOURCE);
    return files.list(StandardLocation.SOURCE_PATH, "", fileKinds, true);
  }

}
