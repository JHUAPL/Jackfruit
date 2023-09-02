package jackfruit.processor;

/*-
 * #%L
 * jackfruit
 * %%
 * Copyright (C) 2023 Johns Hopkins University Applied Physics Lab
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
   * @param typeMirror the return type without any parameters (e.g. List rather than
   *    *     List&lt;String&gt;)
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link List}
   */
  public static boolean isList(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.util.List.class);
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Boolean} or primitive boolean
   */
  public static boolean isBoolean(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Boolean.class)
        || typeMirror.getKind() == TypeKind.BOOLEAN;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Byte} or primitive byte
   */
  public static boolean isByte(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Byte.class)
        || typeMirror.getKind() == TypeKind.BYTE;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Double} or primitive double
   */
  public static boolean isDouble(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Double.class)
        || typeMirror.getKind() == TypeKind.DOUBLE;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Float} or primitive float
   */
  public static boolean isFloat(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Float.class)
        || typeMirror.getKind() == TypeKind.FLOAT;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Integer} or primitive int
   */
  public static boolean isInteger(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Integer.class)
        || typeMirror.getKind() == TypeKind.INT;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Long} or primitive long
   */
  public static boolean isLong(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Long.class)
        || typeMirror.getKind() == TypeKind.LONG;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link Short} or primitive float
   */
  public static boolean isShort(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.Short.class)
        || typeMirror.getKind() == TypeKind.SHORT;
  }

  /**
   * @param typeMirror either {@link AnnotationBundle#erasure()} for the return value, or an element
   *     of {@link AnnotationBundle#typeArgs()} for a parameterized type
   * @param processingEnv Processing environment providing by the tool framework, from {@link
   *     javax.annotation.processing.AbstractProcessor}
   * @return true if this annotated member returns a {@link String}
   */
  public static boolean isString(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    return isClass(typeMirror, processingEnv, java.lang.String.class);
  }

  private static boolean isClass(
      TypeMirror typeMirror, ProcessingEnvironment processingEnv, Class<?> compareTo) {
    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();

    if (elements.getTypeElement(compareTo.getCanonicalName()) == null) {
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              String.format("Cannot recognize %s\n", compareTo.getCanonicalName()));
    }

    return types.isSubtype(
        typeMirror, types.erasure(elements.getTypeElement(compareTo.getCanonicalName()).asType()));
  }
}
