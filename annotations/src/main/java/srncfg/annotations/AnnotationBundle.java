package srncfg.annotations;

import java.util.Optional;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value;

@Value.Immutable
public interface AnnotationBundle {

  public String comment();

  public String defaultValue();

  public String key();

  public Optional<TypeMirror> parserClass();

}
