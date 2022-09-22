package jackfruit.processor;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class ConfigProcessorUtils {
  /**
   * 
   * @param processingEnv
   * @return true if this annotated member returns a {@link List}
   */
  public final static boolean isList(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.util.List.class);
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Boolean} or primitive boolean
   */
  public final static boolean isBoolean(TypeMirror typeMirror,
      ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Boolean.class)
        || typeMirror.getKind() == TypeKind.BOOLEAN;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Byte} or primitive byte
   */
  public final static boolean isByte(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Byte.class)
        || typeMirror.getKind() == TypeKind.BYTE;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Double} or primitive double
   */
  public final static boolean isDouble(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Double.class)
        || typeMirror.getKind() == TypeKind.DOUBLE;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Float} or primitive float
   */
  public final static boolean isFloat(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Float.class)
        || typeMirror.getKind() == TypeKind.FLOAT;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Integer} or primitive int
   */
  public final static boolean isInteger(TypeMirror typeMirror,
      ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Integer.class)
        || typeMirror.getKind() == TypeKind.INT;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Long} or primitive long
   */
  public final static boolean isLong(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Long.class)
        || typeMirror.getKind() == TypeKind.LONG;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link Short} or primitive float
   */
  public final static boolean isShort(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Short.class)
        || typeMirror.getKind() == TypeKind.SHORT;
  }

  /**
   * 
   * @param typeMirror either {@link #erasure()} for the return value, or an element of
   *        {@link #typeArgs()} for a parameterized type
   * @param processingEnv
   * @return true if this annotated member returns a {@link String}
   */
  public final static boolean isString(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.String.class);
  }

  private final static boolean isClass(TypeMirror typeMirror, ProcessingEnvironment processingEnv,
      Class<?> compareTo) {
    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();

    if (elements.getTypeElement(compareTo.getCanonicalName()) == null) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
          String.format("Cannot recognize %s\n", compareTo.getCanonicalName()));
    }

    return types.isSubtype(typeMirror,
        types.erasure(elements.getTypeElement(compareTo.getCanonicalName()).asType()));
  }

}
