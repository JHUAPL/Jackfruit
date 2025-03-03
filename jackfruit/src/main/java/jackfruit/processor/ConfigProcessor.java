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

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import jackfruit.JackfruitVersion;
import jackfruit.annotations.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;

/**
 * Useful references for writing an annotation processor:
 *
 * <ul>
 *   <li><a href=
 *       "https://www.javacodegeeks.com/2015/09/java-annotation-processors.html">https://www.javacodegeeks.com/2015/09/java-annotation-processors.html</a>
 *   <li><a href=
 *       "https://hannesdorfmann.com/annotation-processing/annotationprocessing101/">https://hannesdorfmann.com/annotation-processing/annotationprocessing101/</a>
 *   <li><a href=
 *       "http://www.javatronic.fr/articles/2014/08/31/how_to_make_sure_javac_is_using_a_specific_annotation_processor.html">http://www.javatronic.fr/articles/2014/08/31/how_to_make_sure_javac_is_using_a_specific_annotation_processor.html</a>
 * </ul>
 *
 * @author Hari.Nair@jhuapl.edu
 */
@SupportedAnnotationTypes("jackfruit.annotations.Jackfruit")
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractProcessor {

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    // Dynamically return the latest version available
    return SourceVersion.latest();
  }

  private List<Class<? extends Annotation>> supportedMethodAnnotations;
  private Messager messager;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    supportedMethodAnnotations = new ArrayList<>();
    supportedMethodAnnotations.add(Comment.class);
    supportedMethodAnnotations.add(DefaultValue.class);
    supportedMethodAnnotations.add(Include.class);
    supportedMethodAnnotations.add(Key.class);
    supportedMethodAnnotations.add(ParserClass.class);

    messager = processingEnv.getMessager();

    // find interfaces or abstract classes with the Jackfruit annotation
    List<Element> annotatedElements =
        roundEnv.getElementsAnnotatedWith(Jackfruit.class).stream()
            .filter(
                e ->
                    e.getKind() == ElementKind.INTERFACE
                        || (e.getKind() == ElementKind.CLASS
                            && e.getModifiers().contains(Modifier.ABSTRACT)))
            .collect(Collectors.toList());

    for (Element element : annotatedElements) {

      try {
        if (element instanceof TypeElement annotatedType) {

          Jackfruit configParams = annotatedType.getAnnotation(Jackfruit.class);
          String prefix = configParams.prefix().strip();
          if (!prefix.isEmpty() && !prefix.endsWith(".")) prefix += ".";

          // This is the templatized class with annotations to be processed (e.g.
          // ConfigTemplate)
          TypeVariableName tvn = TypeVariableName.get(annotatedType.getSimpleName().toString());

          // This is the generic class (e.g. ConfigFactory<ConfigTemplate>)
          ParameterizedTypeName ptn =
              ParameterizedTypeName.get(
                  ClassName.get(jackfruit.processor.ConfigFactory.class), tvn);

          // This is the name of the class to create (e.g. ConfigTemplateFactory)
          String factoryName = String.format("%sFactory", annotatedType.getSimpleName());

          OffsetDateTime now = OffsetDateTime.now();
          DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

          AnnotationSpec generated =
              AnnotationSpec.builder(Generated.class)
                  .addMember("value", String.format("\"%s\"", JackfruitVersion.packageName))
                  .addMember("date", String.format("\"%s\"", formatter.format(now)))
                  .addMember(
                      "comments",
                      String.format(
                          "\"version %s built %s\"",
                          JackfruitVersion.version, JackfruitVersion.dateString))
                  .build();

          TypeSpec.Builder classBuilder =
              TypeSpec.classBuilder(factoryName)
                  .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                  .addSuperinterface(ptn)
                  .addAnnotation(generated);
          /*-
          // logger for the generated class
          FieldSpec loggerField = FieldSpec.builder(org.apache.logging.log4j.Logger.class, "logger")
              .initializer("$T.getLogger()", org.apache.logging.log4j.LogManager.class)
              .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).build();
          classBuilder.addField(loggerField);
          */

          // this contains a hierarchy of parent classes
          List<DeclaredType> classHierarchy = new ArrayList<>();
          {
            TypeElement thisElement = annotatedType;
            TypeMirror superClass = thisElement.getSuperclass();
            while (superClass.getKind() == TypeKind.DECLARED) {
              DeclaredType superType = (DeclaredType) superClass;
              // have to use asElement() here
              if (superType.asElement().getAnnotation(Jackfruit.class) == null) {
                break;
              }

              classHierarchy.add(superType);
              thisElement = (TypeElement) superType.asElement();
              superClass = thisElement.getSuperclass();
            }
          }

          Collections.reverse(classHierarchy);
          classHierarchy.add((DeclaredType) annotatedType.asType());

          // create a map of methods annotated with DefaultValue
          Map<Name, ExecutableElement> enclosedMethods = new LinkedHashMap<>();
          // the default values for each method
          Map<Name, AnnotationBundle> defaultAnnotationsMap = new LinkedHashMap<>();
          // Map of included config types
          Map<Name, AnnotationBundle> includedMap = new LinkedHashMap<>();
          for (DeclaredType thisType : classHierarchy) {
            for (Element e : thisType.asElement().getEnclosedElements()) {
              if (e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement ex) {

                if (ex.getAnnotation(Include.class) != null) {
                  AnnotationBundle defaultValues = defaultAnnotationsMap.get(ex.getSimpleName());
                  AnnotationBundle annotationBundle = buildAnnotationBundle(ex, defaultValues);
                  includedMap.put(ex.getSimpleName(), annotationBundle);
                } else if (ex.getAnnotation(DefaultValue.class) != null) {
                  enclosedMethods.put(ex.getSimpleName(), ex);
                  AnnotationBundle defaultValues = defaultAnnotationsMap.get(ex.getSimpleName());
                  AnnotationBundle annotationBundle = buildAnnotationBundle(ex, defaultValues);
                  defaultAnnotationsMap.put(ex.getSimpleName(), annotationBundle);
                }
              }
            }
          }

          // holds the annotation information on each method
          Map<ExecutableElement, AnnotationBundle> annotationsMap = new LinkedHashMap<>();
          for (ExecutableElement e : enclosedMethods.values()) {
            AnnotationBundle defaultValues = defaultAnnotationsMap.get(e.getSimpleName());
            annotationsMap.put(e, buildAnnotationBundle(e, defaultValues));
          }

          // default constructor; initialize prefix
          String prefixMemberName = "prefix";
          classBuilder.addField(String.class, prefixMemberName, Modifier.PRIVATE, Modifier.FINAL);
          MethodSpec constructor =
              MethodSpec.constructorBuilder()
                  .addModifiers(Modifier.PUBLIC)
                  .addStatement("this.$N = $S", prefixMemberName, prefix)
                  .build();
          classBuilder.addMethod(constructor);

          // add a constructor where caller can set prefix
          constructor =
              MethodSpec.constructorBuilder()
                  .addModifiers(Modifier.PUBLIC)
                  .addParameter(String.class, prefixMemberName)
                  .beginControlFlow("if ($N == null)", prefixMemberName)
                  .addStatement("$N = \"\"", prefixMemberName)
                  .endControlFlow()
                  .addStatement("$N = $N.strip()", prefixMemberName, prefixMemberName)
                  .addStatement(
                      "if (!$N.endsWith(\".\")) $N += $S", prefixMemberName, prefixMemberName, ".")
                  .addStatement("this.$N = $N", prefixMemberName, prefixMemberName)
                  .build();
          classBuilder.addMethod(constructor);

          // generate the methods from the interface
          List<MethodSpec> methods = new ArrayList<>();
          for (Method m : ConfigFactory.class.getMethods()) {

            if (m.isDefault()) continue;

            if (m.getName().equals("toConfig")) {
              MethodSpec toConfig =
                  buildToConfig(tvn, m, annotationsMap, includedMap, prefixMemberName);
              methods.add(toConfig);
            }

            if (m.getName().equals("getTemplate")) {
              MethodSpec getTemplate = buildGetTemplate(tvn, m, annotationsMap, includedMap);
              methods.add(getTemplate);
            }

            if (m.getName().equals("fromConfig")) {
              MethodSpec fromConfig =
                  buildFromConfig(tvn, m, annotationsMap, includedMap, prefixMemberName);
              methods.add(fromConfig);
            }
          }

          methods.addAll(buildWithMethods(tvn, annotationsMap, prefixMemberName));

          classBuilder.addMethods(methods);
          TypeSpec thisClass = classBuilder.build();

          // write the source code
          PackageElement pkg = processingEnv.getElementUtils().getPackageOf(annotatedType);
          JavaFileObject jfo =
              processingEnv.getFiler().createSourceFile(pkg.getQualifiedName() + "." + factoryName);
          JavaFile javaFile = JavaFile.builder(pkg.toString(), thisClass).build();
          try (PrintWriter pw = new PrintWriter(jfo.openWriter())) {
            javaFile.writeTo(pw);
          }
          messager.printMessage(
              Diagnostic.Kind.NOTE, String.format("wrote %s", javaFile.toJavaFileObject().toUri()));
        }
      } catch (IOException e1) {
        messager.printMessage(Diagnostic.Kind.ERROR, e1.getLocalizedMessage());
        return false;
      }
    }
    return true;
  }

  /**
   * @param e annotated method
   * @param defaultValues default values for annotations - could be from a parent class
   * @return annotation values for this method
   */
  private AnnotationBundle buildAnnotationBundle(
      ExecutableElement e, AnnotationBundle defaultValues) {

    ImmutableAnnotationBundle.Builder builder = ImmutableAnnotationBundle.builder();
    builder.key(e.getSimpleName().toString());
    builder.comment("");
    builder.defaultValue("");
    if (defaultValues != null) {
      builder.key(defaultValues.key());
      builder.comment(defaultValues.comment());
      builder.defaultValue(defaultValues.defaultValue());
      if (defaultValues.parserClass().isPresent())
        builder.parserClass(defaultValues.parserClass().get());
    }

    Types types = processingEnv.getTypeUtils();
    TypeMirror returnType = e.getReturnType();
    TypeMirror erasure = types.erasure(returnType);
    builder.erasure(erasure);

    List<TypeMirror> typeArgs = new ArrayList<>();
    if (erasure.getKind() == TypeKind.DECLARED) {
      // these are the parameter types for a generic class
      List<? extends TypeMirror> args = ((DeclaredType) returnType).getTypeArguments();
      typeArgs.addAll(args);
    } else if (!erasure.getKind().isPrimitive()) {
      // There is a type argument here
      processingEnv
          .getMessager()
          .printMessage(
              Diagnostic.Kind.ERROR,
              String.format(
                  "Element %s: Unsupported kind %s for type %s!",
                  e, erasure.getKind().toString(), erasure));
    }

    builder.addAllTypeArgs(typeArgs);

    List<Annotation> methodAnnotations = new ArrayList<>();
    for (var a : supportedMethodAnnotations) methodAnnotations.add(e.getAnnotation(a));

    for (Annotation annotation : methodAnnotations) {
      if (annotation == null) continue;

      if (annotation instanceof Key) {
        builder.key(((Key) annotation).value());
      } else if (annotation instanceof Comment) {
        builder.comment(((Comment) annotation).value());
      } else if (annotation instanceof DefaultValue) {
        builder.defaultValue(((DefaultValue) annotation).value());
      } else if (annotation instanceof Include) {
        // do nothing
      } else if (annotation instanceof ParserClass pc) {

        // this works, but there has to be a better way?
        TypeMirror tm;
        try {
          // see
          // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
          tm = processingEnv.getElementUtils().getTypeElement(pc.value().toString()).asType();
        } catch (MirroredTypeException mte) {
          tm = mte.getTypeMirror();
        }
        builder.parserClass(tm);
      } else {
        throw new IllegalArgumentException(
            "Unknown annotation type " + annotation.getClass().getSimpleName());
      }
    }

    AnnotationBundle bundle = builder.build();
    if (ConfigProcessorUtils.isList(bundle.erasure(), processingEnv) && bundle.typeArgs().isEmpty())
      messager.printMessage(
          Diagnostic.Kind.ERROR,
          String.format("No parameter type for List on method %s!", e.getSimpleName()));
    return bundle;
  }

  /**
   * Create the {@link ConfigFactory#toConfig(Object, PropertiesConfigurationLayout)} method.
   *
   * @param tvn the type variable name representing the generic type of the object being processed.
   * @param m the method being processed.
   * @param annotationsMap a map containing methods and associated metadata.
   * @param includedMap a map containing classes to be added from an {@link Include} annotation.
   * @param prefixMemberName a string representing the prefix when generating configuration keys.
   * @return a {@link MethodSpec} instance representing the generated method for converting an
   *     object to an Apache Commons {@link Configuration}.
   */
  private MethodSpec buildToConfig(
      TypeVariableName tvn,
      Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap,
      Map<Name, AnnotationBundle> includedMap,
      String prefixMemberName) {
    ParameterSpec ps = ParameterSpec.builder(tvn, "t").build();
    ParameterSpec layout =
        ParameterSpec.builder(
                TypeVariableName.get(
                    org.apache.commons.configuration2.PropertiesConfigurationLayout.class
                        .getCanonicalName()),
                "layout")
            .build();

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(m.getName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(m.getGenericReturnType());
    methodBuilder.addParameter(ps);
    methodBuilder.addParameter(layout);

    methodBuilder.addStatement(
        "$T config = new $T()",
        org.apache.commons.configuration2.PropertiesConfiguration.class,
        org.apache.commons.configuration2.PropertiesConfiguration.class);
    methodBuilder.addStatement("config.setLayout($N)", layout);

    boolean needBlank = true;
    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle ab = annotationsMap.get(method);
      String key = ab.key();
      if (needBlank) {
        methodBuilder.addStatement(
            "$N.setBlankLinesBefore($N + $S, 1)", layout, prefixMemberName, key);
        needBlank = false;
      }

      TypeMirror parser;
      String parserName = null;
      if (ab.parserClass().isPresent()) {
        parser = ab.parserClass().get();
        parserName = method.getSimpleName() + "Parser";
        methodBuilder.addStatement("$T " + parserName + " = new $T()", parser, parser);
      }

      if (ConfigProcessorUtils.isList(ab.erasure(), processingEnv)) {
        // if it's a list, store a List<String> in the Apache configuration
        TypeVariableName stringType = TypeVariableName.get(java.lang.String.class.getName());
        ParameterizedTypeName listType =
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), stringType);
        ParameterizedTypeName arrayListType =
            ParameterizedTypeName.get(ClassName.get(java.util.ArrayList.class), stringType);
        String listName = method.getSimpleName() + "List";
        methodBuilder.addStatement("$T " + listName + " = new $T()", listType, arrayListType);
        methodBuilder.beginControlFlow("for (var element : t.$L())", method.getSimpleName());
        if (ab.parserClass().isPresent()) {
          methodBuilder.addStatement("$L.add($L.toString(element))", listName, parserName);
        } else {
          TypeMirror typeArg = ab.typeArgs().get(0);
          if (ConfigProcessorUtils.isByte(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Byte.class);
          if (ConfigProcessorUtils.isBoolean(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Boolean.class);
          if (ConfigProcessorUtils.isDouble(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Double.class);
          if (ConfigProcessorUtils.isFloat(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Float.class);
          if (ConfigProcessorUtils.isInteger(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Integer.class);
          if (ConfigProcessorUtils.isLong(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Long.class);
          if (ConfigProcessorUtils.isShort(typeArg, processingEnv))
            methodBuilder.addStatement(
                "$L.add($T.toString(element))", listName, java.lang.Short.class);
          if (ConfigProcessorUtils.isString(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add(element)", listName);
        }
        methodBuilder.endControlFlow();
        methodBuilder.addStatement(
            "config.setProperty($N + $S, $L)", prefixMemberName, key, listName);
      } else {
        if (ab.parserClass().isPresent()) {
          // store the serialized string as the property
          methodBuilder.addStatement(
              "config.setProperty($N + $S, $L.toString($N.$L()))",
              prefixMemberName,
              key,
              parserName,
              ps,
              method.getSimpleName());
        } else {
          methodBuilder.addStatement(
              "config.setProperty($N + $S, t.$L())", prefixMemberName, key, method.getSimpleName());
        }
      }

      // add the comment
      if (!ab.comment().isEmpty()) {
        String commentName = String.format("%sComment", method.getSimpleName());
        methodBuilder.addStatement("$T $L = $S", String.class, commentName, ab.comment());
        methodBuilder.addStatement(
            "$N.setComment($N + $S, $L)", layout, prefixMemberName, key, commentName);
      }
    }

    // add included classes
    Types types = processingEnv.getTypeUtils();
    for (Name name : includedMap.keySet()) {
      AnnotationBundle bundle = includedMap.get(name);
      String className = types.asElement(bundle.erasure()).getSimpleName().toString();
      methodBuilder.addStatement(
          "config.append(new $LFactory().toConfig(t.$L(), layout))", className, name);
    }

    methodBuilder.addCode("return config;");

    return methodBuilder.build();
  }

  /**
   * Create the {@link ConfigFactory#getTemplate()} method.
   *
   * @param tvn the type variable name representing the generic type of the object being processed.
   * @param m the method being processed.
   * @param annotationsMap a map containing methods and associated metadata.
   * @return a {@link MethodSpec} that returns a template object, populated by the default values.
   */
  private MethodSpec buildGetTemplate(
      TypeVariableName tvn,
      Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap,
      Map<Name, AnnotationBundle> includedMap) {

    Types types = processingEnv.getTypeUtils();

    // this builds the getTemplate() method
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(m.getName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(tvn);
    TypeSpec.Builder typeBuilder = TypeSpec.anonymousClassBuilder("").addSuperinterface(tvn);

    for (Name name : includedMap.keySet()) {
      AnnotationBundle bundle = includedMap.get(name);

      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(name.toString())
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override.class)
              .returns(TypeName.get(bundle.erasure()))
              .addJavadoc(bundle.comment());

      builder.addStatement(
          "return new $LFactory().getTemplate()",
          types.asElement(bundle.erasure()).getSimpleName());

      typeBuilder.addMethod(builder.build());
    }

    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle bundle = annotationsMap.get(method);

      // this builds the method on the anonymous class
      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(method.getSimpleName().toString())
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override.class)
              .returns(TypeName.get(method.getReturnType()))
              .addJavadoc(bundle.comment());

      TypeMirror parser;
      String parserName = null;
      if (bundle.parserClass().isPresent()) {
        parser = bundle.parserClass().get();
        parserName = method.getSimpleName() + "Parser";
        builder.addStatement("$T $L = new $T()", parser, parserName, parser);
      }

      if (ConfigProcessorUtils.isList(bundle.erasure(), processingEnv)) {

        TypeName argType = TypeName.get(bundle.typeArgs().get(0));
        ParameterizedTypeName listType =
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), argType);
        ParameterizedTypeName arrayListType =
            ParameterizedTypeName.get(ClassName.get(java.util.ArrayList.class), argType);
        String listName = method.getSimpleName() + "List";
        builder.addStatement("$T " + listName + " = new $T()", listType, arrayListType);

        builder.addStatement(
            "String [] parts = ($S).split($S)", bundle.defaultValue(), "[\\n\\r\\s]+");
        builder.beginControlFlow("for (String part : parts)");
        builder.beginControlFlow("if (part.trim().length() > 0)");
        if (bundle.parserClass().isPresent()) {
          builder.addStatement("$L.add($L.fromString(part))", listName, parserName);
        } else {
          TypeMirror typeArg = bundle.typeArgs().get(0);
          if (ConfigProcessorUtils.isByte(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Byte.class);
          if (ConfigProcessorUtils.isBoolean(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Boolean.class);
          if (ConfigProcessorUtils.isDouble(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Double.class);
          if (ConfigProcessorUtils.isFloat(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Float.class);
          if (ConfigProcessorUtils.isInteger(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Integer.class);
          if (ConfigProcessorUtils.isLong(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Long.class);
          if (ConfigProcessorUtils.isShort(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Short.class);
          if (ConfigProcessorUtils.isString(typeArg, processingEnv))
            builder.addStatement("$L.add(part)", listName);
        }
        builder.endControlFlow();
        builder.endControlFlow();
        builder.addStatement("return $L", listName);

      } else {
        if (bundle.parserClass().isPresent()) {
          builder.addStatement("return $L.fromString($S)", parserName, bundle.defaultValue());
        } else {
          if (ConfigProcessorUtils.isString(bundle.erasure(), processingEnv)) {
            builder.addStatement("return $S", bundle.defaultValue());
          } else {
            if (bundle.defaultValue().trim().isEmpty()) {
              processingEnv
                  .getMessager()
                  .printMessage(
                      Diagnostic.Kind.ERROR,
                      String.format(
                          "Default value on method %s is blank!", method.getSimpleName()));
            }
            builder.addStatement("return $L", bundle.defaultValue());
          }
        }
      }
      typeBuilder.addMethod(builder.build());
    }

    methodBuilder.addStatement("return $L", typeBuilder.build());
    return methodBuilder.build();
  }

  /**
   * Create the {@link ConfigFactory#fromConfig(Configuration)} method.
   *
   * @param tvn the type variable name representing the generic type of the object being processed.
   * @param m the method being processed.
   * @param annotationsMap a map containing methods and associated metadata.
   * @param includedMap a map containing classes to be added from an {@link Include} annotation.
   * @return a {@link MethodSpec} instance representing the generated method for converting an
   *     Apache Commons {@link Configuration} to an object.
   */
  private MethodSpec buildFromConfig(
      TypeVariableName tvn,
      Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap,
      Map<Name, AnnotationBundle> includedMap,
      String prefix) {

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(m.getName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(tvn)
            .addParameter(org.apache.commons.configuration2.Configuration.class, "config");

    TypeSpec.Builder typeBuilder = TypeSpec.anonymousClassBuilder("").addSuperinterface(tvn);

    Types types = processingEnv.getTypeUtils();
    for (Name name : includedMap.keySet()) {
      AnnotationBundle bundle = includedMap.get(name);

      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(name.toString())
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override.class)
              .returns(TypeName.get(bundle.erasure()))
              .addJavadoc(bundle.comment());

      builder.addStatement(
          "return new $LFactory().fromConfig(config)",
          types.asElement(bundle.erasure()).getSimpleName());

      typeBuilder.addMethod(builder.build());
    }

    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle bundle = annotationsMap.get(method);
      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(method.getSimpleName().toString())
              .addModifiers(Modifier.PUBLIC)
              .addAnnotation(Override.class)
              .returns(TypeName.get(method.getReturnType()))
              .addJavadoc(bundle.comment());

      builder.addStatement("String key = $N + $S", prefix, bundle.key());
      builder
          .beginControlFlow("if (!config.containsKey(key))")
          .addStatement("throw new $T($S + key)", RuntimeException.class, "No such key ")
          .endControlFlow();

      TypeMirror parser;
      String parserName = null;
      if (bundle.parserClass().isPresent()) {
        parser = bundle.parserClass().get();
        parserName = method.getSimpleName() + "Parser";
        builder.addStatement("$T $L = new $T()", parser, parserName, parser);
      }

      if (ConfigProcessorUtils.isList(bundle.erasure(), processingEnv)) {
        TypeName argType = TypeName.get(bundle.typeArgs().get(0));
        ParameterizedTypeName listType =
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), argType);
        ParameterizedTypeName arrayListType =
            ParameterizedTypeName.get(ClassName.get(java.util.ArrayList.class), argType);
        String listName = method.getSimpleName() + "List";
        builder.addStatement("$T " + listName + " = new $T()", listType, arrayListType);
        builder.addStatement("String [] parts = config.getStringArray(key)");
        builder.beginControlFlow("for (String part : parts)");
        builder.beginControlFlow("if (part.trim().length() > 0)");
        if (bundle.parserClass().isPresent()) {
          builder.addStatement("$L.add($L.fromString(part))", listName, parserName);
        } else {
          TypeMirror typeArg = bundle.typeArgs().get(0);
          if (ConfigProcessorUtils.isByte(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Byte.class);
          if (ConfigProcessorUtils.isBoolean(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Boolean.class);
          if (ConfigProcessorUtils.isDouble(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Double.class);
          if (ConfigProcessorUtils.isFloat(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Float.class);
          if (ConfigProcessorUtils.isInteger(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Integer.class);
          if (ConfigProcessorUtils.isLong(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Long.class);
          if (ConfigProcessorUtils.isShort(typeArg, processingEnv))
            builder.addStatement("$L.add($T.valueOf(part))", listName, java.lang.Short.class);
          if (ConfigProcessorUtils.isString(typeArg, processingEnv))
            builder.addStatement("$L.add(part)", listName);
        }
        builder.endControlFlow();
        builder.endControlFlow();
        builder.addStatement("return $L", listName);
      } else {
        if (bundle.parserClass().isPresent()) {
          builder.addStatement("return $L.fromString(config.getString(key))", parserName);
        } else {
          if (ConfigProcessorUtils.isBoolean(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getBoolean(key)");
          } else if (ConfigProcessorUtils.isByte(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getByte(key)");
          } else if (ConfigProcessorUtils.isDouble(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getDouble(key)");
          } else if (ConfigProcessorUtils.isFloat(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getFloat(key)");
          } else if (ConfigProcessorUtils.isInteger(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getInt(key)");
          } else if (ConfigProcessorUtils.isLong(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getLong(key)");
          } else if (ConfigProcessorUtils.isShort(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getShort(key)");
          } else if (ConfigProcessorUtils.isString(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getString(key)");
          } else {
            processingEnv
                .getMessager()
                .printMessage(
                    Diagnostic.Kind.ERROR,
                    "Can't handle return type " + m.getReturnType().getCanonicalName());
          }
        }
      }
      typeBuilder.addMethod(builder.build());
    }
    methodBuilder.addStatement("return $L", typeBuilder.build());

    return methodBuilder.build();
  }

  /**
   * Create the "with" methods of the factory class.
   *
   * @param tvn the type variable name representing the generic type of the object being processed.
   * @param annotationsMap a map containing methods and associated metadata.
   * @param prefixMemberName a string representing the prefix when generating configuration keys.
   * @return A list of methods that allow the user to change values.
   */
  private List<MethodSpec> buildWithMethods(
      TypeVariableName tvn,
      Map<ExecutableElement, AnnotationBundle> annotationsMap,
      String prefixMemberName) {

    List<MethodSpec> withMethods = new ArrayList<>();

    ParameterSpec ps = ParameterSpec.builder(tvn, "t").build();

    for (ExecutableElement method : annotationsMap.keySet()) {
      String methodName = method.getSimpleName().toString();
      String camelCase =
          String.format(
              "with%s%s", methodName.substring(0, 1).toUpperCase(), methodName.substring(1));
      TypeName propertiesConfigurationClass =
          TypeName.get(org.apache.commons.configuration2.PropertiesConfiguration.class);

      // method with object passed in as an argument
      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(camelCase)
              .addModifiers(Modifier.PUBLIC)
              .returns(propertiesConfigurationClass);
      builder.addJavadoc("Replace the value of " + methodName);
      builder.addParameter(ps);
      builder.addParameter(
          ParameterSpec.builder(TypeName.get(method.getReturnType()), "replaceValue").build());
      builder.addStatement("$T config = toConfig($N)", propertiesConfigurationClass, ps);
      builder.addStatement(String.format("return %s(config, replaceValue)", camelCase));
      withMethods.add(builder.build());

      // method with PropertiesConfiguration passed in as an argument
      builder =
          MethodSpec.methodBuilder(camelCase)
              .addModifiers(Modifier.PUBLIC)
              .returns(propertiesConfigurationClass);
      builder.addJavadoc("Replace the value of " + methodName);
      builder.addParameter(ParameterSpec.builder(propertiesConfigurationClass, "config").build());
      builder.addParameter(
          ParameterSpec.builder(TypeName.get(method.getReturnType()), "replaceValue").build());

      AnnotationBundle ab = annotationsMap.get(method);
      String key = ab.key();

      TypeMirror parser;
      String parserName = null;
      if (ab.parserClass().isPresent()) {
        parser = ab.parserClass().get();
        parserName = method.getSimpleName() + "Parser";
        builder.addStatement("$T " + parserName + " = new $T()", parser, parser);
      }

      if (ConfigProcessorUtils.isList(ab.erasure(), processingEnv)) {
        // if it's a list, store a List<String> in the Apache configuration
        TypeVariableName stringType = TypeVariableName.get(java.lang.String.class.getName());
        ParameterizedTypeName listType =
            ParameterizedTypeName.get(ClassName.get(java.util.List.class), stringType);
        ParameterizedTypeName arrayListType =
            ParameterizedTypeName.get(ClassName.get(java.util.ArrayList.class), stringType);
        String listName = method.getSimpleName() + "List";
        builder.addStatement("$T " + listName + " = new $T()", listType, arrayListType);
        builder.beginControlFlow("for (var element : replaceValue)");
        if (ab.parserClass().isPresent()) {
          builder.addStatement("$L.add($L.toString(element))", listName, parserName);
        } else {
          TypeMirror typeArg = ab.typeArgs().get(0);
          if (ConfigProcessorUtils.isByte(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Byte.class);
          if (ConfigProcessorUtils.isBoolean(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Boolean.class);
          if (ConfigProcessorUtils.isDouble(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Double.class);
          if (ConfigProcessorUtils.isFloat(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Float.class);
          if (ConfigProcessorUtils.isInteger(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Integer.class);
          if (ConfigProcessorUtils.isLong(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Long.class);
          if (ConfigProcessorUtils.isShort(typeArg, processingEnv))
            builder.addStatement("$L.add($T.toString(element))", listName, java.lang.Short.class);
          if (ConfigProcessorUtils.isString(typeArg, processingEnv))
            builder.addStatement("$L.add(element)", listName);
        }
        builder.endControlFlow();
        builder.addStatement("config.setProperty($N + $S, $L)", prefixMemberName, key, listName);
      } else {
        if (ab.parserClass().isPresent()) {
          // store the serialized string as the property
          builder.addStatement(
              "config.setProperty($N + $S, $L.toString(replaceValue))",
              prefixMemberName,
              key,
              parserName);
        } else {
          builder.addStatement("config.setProperty($N + $S, replaceValue)", prefixMemberName, key);
        }
      }

      builder.addStatement("return config");

      withMethods.add(builder.build());
    }

    return withMethods;
  }
}
