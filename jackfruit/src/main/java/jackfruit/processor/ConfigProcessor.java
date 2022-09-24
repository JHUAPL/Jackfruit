package jackfruit.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.apache.commons.configuration2.Configuration;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.Key;
import jackfruit.annotations.ParserClass;

/**
 * https://www.javacodegeeks.com/2015/09/java-annotation-processors.html
 * 
 * @author nairah1
 *
 */
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("jackfruit.annotations.Jackfruit")
@AutoService(Processor.class)
public class ConfigProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

    List<Class<? extends Annotation>> supportedMethodAnnotations = new ArrayList<>();
    supportedMethodAnnotations.add(Comment.class);
    supportedMethodAnnotations.add(DefaultValue.class);
    supportedMethodAnnotations.add(Key.class);
    supportedMethodAnnotations.add(ParserClass.class);

    Messager messager = processingEnv.getMessager();

    // find interfaces or abstract classes with the Jackfruit annotation
    List<Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Jackfruit.class).stream()
        .filter(e -> e.getKind() == ElementKind.INTERFACE
            || (e.getKind() == ElementKind.CLASS && e.getModifiers().contains(Modifier.ABSTRACT)))
        .collect(Collectors.toList());

    for (Element element : annotatedElements) {

      try {
        if (element instanceof TypeElement) {
          TypeElement annotatedType = (TypeElement) element;

          Jackfruit configParams = (Jackfruit) annotatedType.getAnnotation(Jackfruit.class);
          String prefix = configParams.prefix();
          if (prefix.length() > 0)
            prefix += ".";

          // This is the templatized class with annotations to be processed (e.g.
          // ConfigTemplate)
          TypeVariableName tvn = TypeVariableName.get(annotatedType.getSimpleName().toString());

          // This is the generic class (e.g. ConfigFactory<ConfigTemplate>)
          ParameterizedTypeName ptn = ParameterizedTypeName
              .get(ClassName.get(jackfruit.processor.ConfigFactory.class), tvn);

          // This is the name of the class to create (e.g. ConfigTemplateFactory)
          String factoryName = String.format("%sFactory", annotatedType.getSimpleName());

          TypeSpec.Builder classBuilder = TypeSpec.classBuilder(factoryName)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL).addSuperinterface(ptn);
          /*-
          // logger for the generated class
          FieldSpec loggerField = FieldSpec.builder(org.apache.logging.log4j.Logger.class, "logger")
              .initializer("$T.getLogger()", org.apache.logging.log4j.LogManager.class)
              .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).build();
          classBuilder.addField(loggerField);
          */

          // create a list of methods annotated with DefaultValue - ignore everything else
          List<ExecutableElement> enclosedMethods = new ArrayList<>();
          for (Element e : annotatedType.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD && e.getAnnotation(DefaultValue.class) != null
                && e instanceof ExecutableElement)
              enclosedMethods.add((ExecutableElement) e);
          }

          // holds the annotation information on each method
          Map<ExecutableElement, AnnotationBundle> annotationsMap = new LinkedHashMap<>();
          for (ExecutableElement e : enclosedMethods) {

            ImmutableAnnotationBundle.Builder builder = ImmutableAnnotationBundle.builder();
            builder.key(e.getSimpleName().toString());
            builder.comment("");

            Types types = processingEnv.getTypeUtils();
            TypeMirror returnType = e.getReturnType();
            TypeMirror erasure = types.erasure(returnType);
            builder.erasure(erasure);

            List<TypeMirror> typeArgs = new ArrayList<>();
            if (erasure.getKind() == TypeKind.DECLARED) {
              // these are the parameter types for a generic class
              List<? extends TypeMirror> args = ((DeclaredType) returnType).getTypeArguments();
              typeArgs.addAll(args);
            } else if (erasure.getKind().isPrimitive()) {
              // no type arguments here
            } else {
              processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                  String.format("Unsupported kind %s for type %s!", erasure.getKind().toString(),
                      erasure.toString()));
            }

            builder.addAllTypeArgs(typeArgs);

            String defaultValue = null;

            List<Annotation> methodAnnotations = new ArrayList<>();
            for (var a : supportedMethodAnnotations)
              methodAnnotations.add(e.getAnnotation(a));

            for (Annotation annotation : methodAnnotations) {
              if (annotation == null)
                continue;

              if (annotation instanceof Key) {
                builder.key(((Key) annotation).value());
              } else if (annotation instanceof Comment) {
                builder.comment(((Comment) annotation).value());
              } else if (annotation instanceof DefaultValue) {
                DefaultValue d = (DefaultValue) annotation;
                defaultValue = d.value();
              } else if (annotation instanceof ParserClass) {
                ParserClass pc = (ParserClass) annotation;

                // this works, but there has to be a better way?
                TypeMirror tm;
                try {
                  // see
                  // https://stackoverflow.com/questions/7687829/java-6-annotation-processing-getting-a-class-from-an-annotation
                  tm = processingEnv.getElementUtils().getTypeElement(pc.value().toString())
                      .asType();
                } catch (MirroredTypeException mte) {
                  tm = mte.getTypeMirror();
                }
                builder.parserClass(tm);
              } else {
                throw new IllegalArgumentException(
                    "Unknown annotation type " + annotation.getClass().getSimpleName());
              }
            }

            // if a method does not have a default value, it will not be included in the generated
            // code
            if (defaultValue == null) {
              messager.printMessage(Diagnostic.Kind.WARNING,
                  String.format("No default value on method %s!", e.getSimpleName()));
              continue;
            }

            builder.defaultValue(defaultValue);
            AnnotationBundle bundle = builder.build();
            if (ConfigProcessorUtils.isList(bundle.erasure(), processingEnv)
                && bundle.typeArgs().size() == 0)
              messager.printMessage(Diagnostic.Kind.ERROR,
                  String.format("No parameter type for List on method %s!", e.getSimpleName()));
            annotationsMap.put(e, bundle);
          }

          // generate the methods from the interface
          List<MethodSpec> methods = new ArrayList<>();
          for (Method m : ConfigFactory.class.getMethods()) {

            if (m.isDefault())
              continue;

            if (m.getName().equals("toConfig")) {
              MethodSpec toConfig = buildToConfig(tvn, m, annotationsMap, prefix);
              methods.add(toConfig);
            }

            if (m.getName().equals("getTemplate")) {
              MethodSpec getTemplate = buildGetTemplate(tvn, m, annotationsMap, prefix);
              methods.add(getTemplate);
            }

            if (m.getName().equals("fromConfig")) {
              MethodSpec fromConfig = buildFromConfig(tvn, m, annotationsMap, prefix);
              methods.add(fromConfig);
            }

          }
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
          messager.printMessage(Diagnostic.Kind.NOTE,
              String.format("wrote %s", javaFile.toJavaFileObject().toUri()));
        }
      } catch (IOException e1) {
        messager.printMessage(Diagnostic.Kind.ERROR, e1.getLocalizedMessage());
        e1.printStackTrace();
      }
    }
    return true;

  }

  /**
   * Build the method to generate an Apache Commons {@link Configuration}
   * 
   * @param tvn
   * @param m
   * @param annotationsMap
   * @param prefix
   * @return
   */
  private MethodSpec buildToConfig(TypeVariableName tvn, Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap, String prefix) {
    ParameterSpec ps = ParameterSpec.builder(tvn, "t").build();
    ParameterSpec layout = ParameterSpec.builder(TypeVariableName.get(
        org.apache.commons.configuration2.PropertiesConfigurationLayout.class.getCanonicalName()),
        "layout").build();
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(m.getName()).addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC).returns(m.getGenericReturnType());
    methodBuilder.addParameter(ps);
    methodBuilder.addParameter(layout);

    methodBuilder.addStatement("$T config = new $T()",
        org.apache.commons.configuration2.PropertiesConfiguration.class,
        org.apache.commons.configuration2.PropertiesConfiguration.class);
    methodBuilder.addStatement("config.setLayout($N)", layout);

    boolean needBlank = true;
    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle ab = annotationsMap.get(method);
      String key = prefix + ab.key();
      if (needBlank) {
        methodBuilder.addStatement("$N.setBlancLinesBefore($S, 1)", layout, key);
        needBlank = false;
      }

      TypeMirror parser = null;
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
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Byte.class);
          if (ConfigProcessorUtils.isBoolean(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Boolean.class);
          if (ConfigProcessorUtils.isDouble(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Double.class);
          if (ConfigProcessorUtils.isFloat(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Float.class);
          if (ConfigProcessorUtils.isInteger(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Integer.class);
          if (ConfigProcessorUtils.isLong(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Long.class);
          if (ConfigProcessorUtils.isShort(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add($T.toString(element))", listName,
                java.lang.Short.class);
          if (ConfigProcessorUtils.isString(typeArg, processingEnv))
            methodBuilder.addStatement("$L.add(element)", listName);
        }
        methodBuilder.endControlFlow();
        methodBuilder.addStatement("config.setProperty($S, $L)", key, listName);
      } else {
        if (ab.parserClass().isPresent()) {
          // store the serialized string as the property
          methodBuilder.addStatement("config.setProperty($S, $L.toString($N.$L()))", key,
              parserName, ps, method.getSimpleName());
        } else {
          methodBuilder.addStatement("config.setProperty($S, t.$L())", key, method.getSimpleName());
        }
      }

      // add the comment
      if (ab.comment().length() > 0) {
        String commentName = String.format("%sComment", method.getSimpleName());
        methodBuilder.addStatement("$T $L = $S", String.class, commentName, ab.comment());
        methodBuilder.addStatement("$N.setComment($S, $L)", layout, key, commentName);
      }
    }

    methodBuilder.addCode("return config;");

    return methodBuilder.build();
  }

  private MethodSpec buildGetTemplate(TypeVariableName tvn, Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap, String prefix) {

    // this builds the getTemplate() method
    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(m.getName())
        .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(tvn);
    TypeSpec.Builder typeBuilder = TypeSpec.anonymousClassBuilder("").addSuperinterface(tvn);

    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle bundle = annotationsMap.get(method);

      // this builds the method on the anonymous class
      MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
          .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
          .returns(TypeName.get(method.getReturnType())).addJavadoc(bundle.comment());

      TypeMirror parser = null;
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

        builder.addStatement("String [] parts = ($S).split($S)", bundle.defaultValue(),
            "[\\n\\r\\s]+");
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
            if (bundle.defaultValue().trim().length() == 0) {
              processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                  String.format("Default value on method %s is blank!", method.getSimpleName()));
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

  private MethodSpec buildFromConfig(TypeVariableName tvn, Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap, String prefix) {

    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(m.getName())
        .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(tvn)
        .addParameter(org.apache.commons.configuration2.Configuration.class, "config");

    TypeSpec.Builder typeBuilder = TypeSpec.anonymousClassBuilder("").addSuperinterface(tvn);
    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle bundle = annotationsMap.get(method);
      MethodSpec.Builder builder = MethodSpec.methodBuilder(method.getSimpleName().toString())
          .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class)
          .returns(TypeName.get(method.getReturnType())).addJavadoc(bundle.comment());

      TypeMirror parser = null;
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
        builder.addStatement("String [] parts = config.getStringArray($S)", prefix + bundle.key());
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
          builder.addStatement("return $L.fromString(config.getString($S))", parserName,
              prefix + bundle.key());
        } else {
          if (ConfigProcessorUtils.isBoolean(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getBoolean($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isByte(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getByte($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isDouble(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getDouble($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isFloat(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getFloat($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isInteger(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getInt($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isLong(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getLong($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isShort(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getShort($S)", prefix + bundle.key());
          } else if (ConfigProcessorUtils.isString(bundle.erasure(), processingEnv)) {
            builder.addStatement("return config.getString($S)", prefix + bundle.key());
          } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Can't handle return type " + m.getReturnType().getCanonicalName());
          }
        }
      }
      typeBuilder.addMethod(builder.build());
    }
    methodBuilder.addStatement("return $L", typeBuilder.build());

    return methodBuilder.build();
  }

}
