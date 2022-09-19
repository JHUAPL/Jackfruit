package jackfruit.demo;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.Test;
import crucible.crust.logging.Log4j2Configurator;
import jackfruit.processor.ConfigProcessor;

/**
 * From <a href=
 * "https://stackoverflow.com/questions/21427301/debugging-annotation-processors-in-eclipse">https://stackoverflow.com/questions/21427301/debugging-annotation-processors-in-eclipse</a>
 * 
 * @author nairah1
 *
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
      task.setProcessors(Arrays.asList(new ConfigProcessor()));

      task.call();
    }
  }

  private Iterable<JavaFileObject> getSourceFiles(String p_path) throws Exception {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager files = compiler.getStandardFileManager(null, null, null);

    files.setLocation(StandardLocation.SOURCE_PATH, Arrays.asList(new File(p_path)));

    Set<Kind> fileKinds = Collections.singleton(Kind.SOURCE);
    return files.list(StandardLocation.SOURCE_PATH, "", fileKinds, true);
  }

}
