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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import jackfruit.annotations.Comment;
import jackfruit.annotations.Jackfruit;
import jackfruit.annotations.DefaultValue;
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

    List<Element> annotatedInterfaces = roundEnv.getElementsAnnotatedWith(Jackfruit.class).stream()
        .filter(e -> e.getKind() == ElementKind.INTERFACE).collect(Collectors.toList());

    for (Element element : annotatedInterfaces) {

      try {
        if (element instanceof TypeElement) {
          TypeElement annotatedType = (TypeElement) element;

          Jackfruit configParams = (Jackfruit) annotatedType.getAnnotation(Jackfruit.class);
          String prefix = configParams.prefix();
          if (prefix.length() > 0)
            prefix += ".";

          // This is the templatized class; e.g. "ConfigTemplate"
          TypeVariableName tvn = TypeVariableName.get(annotatedType.getSimpleName().toString());

          // This is the generic class; e.g. "ConfigFactory<TestConfig>"
          ParameterizedTypeName ptn = ParameterizedTypeName
              .get(ClassName.get(jackfruit.processor.ConfigFactory.class), tvn);

          String factoryName = String.format("%sFactory", annotatedType.getSimpleName());

          TypeSpec.Builder classBuilder = TypeSpec.classBuilder(factoryName)
              .addModifiers(Modifier.PUBLIC, Modifier.FINAL).addSuperinterface(ptn);
          FieldSpec loggerField = FieldSpec.builder(org.apache.logging.log4j.Logger.class, "logger")
              .initializer("$T.getLogger()", org.apache.logging.log4j.LogManager.class)
              .addModifiers(Modifier.PRIVATE, Modifier.FINAL, Modifier.STATIC).build();
          classBuilder.addField(loggerField);

          List<ExecutableElement> enclosedMethods = new ArrayList<>();
          for (Element e : annotatedType.getEnclosedElements()) {
            if (e.getKind() == ElementKind.METHOD && e instanceof ExecutableElement)
              enclosedMethods.add((ExecutableElement) e);
          }
          for (ExecutableElement e : enclosedMethods) {
            messager.printMessage(Diagnostic.Kind.NOTE,
                String.format("Annotated method %s\n", e.getSimpleName()));

            for (var a : supportedMethodAnnotations) {
              if (e.getAnnotation(a) != null)
                messager.printMessage(Diagnostic.Kind.NOTE,
                    String.format("annotated with %s\n", a.getName()));
            }

          }
          Map<ExecutableElement, AnnotationBundle> annotationsMap = new LinkedHashMap<>();
          for (ExecutableElement e : enclosedMethods) {
            ImmutableAnnotationBundle.Builder builder = ImmutableAnnotationBundle.builder();
            builder.key(e.getSimpleName().toString());
            builder.comment("");
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

            if (defaultValue == null) {
              messager.printMessage(Diagnostic.Kind.ERROR,
                  String.format("No default value on method %s!", e.getSimpleName()));
              continue;
            }

            builder.defaultValue(defaultValue);
            annotationsMap.put(e, builder.build());
          }

          List<MethodSpec> methods = new ArrayList<>();
          for (Method m : ConfigFactory.class.getMethods()) {

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
        methodBuilder.addStatement(String.format("$N.setBlancLinesBefore(\"%s\", 1)", key), layout);
        needBlank = false;
      }
      if (ab.parserClass().isPresent()) {
        TypeMirror parser = ab.parserClass().get();
        String parserName = method.getSimpleName() + "parser";
        methodBuilder.addStatement("$T " + parserName + " = new $T()", parser, parser);
        methodBuilder.addStatement(String.format("config.setProperty(\"%s\", %s.toString($N.%s()))",
            key, parserName, method.getSimpleName()), ps);
      } else {
        methodBuilder.addStatement(
            String.format("config.setProperty(\"%s\", t.%s())", key, method.getSimpleName()));
      }
      if (ab.comment().length() > 0)
        methodBuilder.addStatement(
            String.format("$N.setComment(\"%s\", \"%s\")", key, ab.comment()), layout);
    }


    methodBuilder.addCode("return config;");

    return methodBuilder.build();
  }

  private MethodSpec buildGetTemplate(TypeVariableName tvn, Method m,
      Map<ExecutableElement, AnnotationBundle> annotationsMap, String prefix) {

    MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(m.getName())
        .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(tvn);
    TypeSpec.Builder typeBuilder = TypeSpec.anonymousClassBuilder("").addSuperinterface(tvn);

    for (ExecutableElement method : annotationsMap.keySet()) {
      AnnotationBundle bundle = annotationsMap.get(method);

      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(method.getSimpleName().toString()).addModifiers(Modifier.PUBLIC)
              .returns(TypeName.get(method.getReturnType())).addJavadoc(bundle.comment());

      if (bundle.parserClass().isPresent()) {

        // it's a custom class

        TypeMirror parser = bundle.parserClass().get();
        builder.addStatement("$T parser = new $T()", parser, parser);
        builder
            .addStatement(String.format("return parser.fromString(\"%s\")", bundle.defaultValue()));
      } else {
        // it's a built in type
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        TypeMirror returnType = method.getReturnType();
        if (types.isAssignable(returnType, elements.getTypeElement("java.lang.String").asType())) {
          // it's a string
          builder.addStatement(String.format("return \"%s\"", bundle.defaultValue()));
        } else {
          TypeKind kind = method.getReturnType().getKind();
          if (kind.isPrimitive()) {
            builder.addStatement(String.format("return %s", bundle.defaultValue()));
          } else {
            // assume it's boxed?
            builder.addStatement(String.format("return %s", bundle.defaultValue()));
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
      MethodSpec.Builder builder =
          MethodSpec.methodBuilder(method.getSimpleName().toString()).addModifiers(Modifier.PUBLIC)
              .returns(TypeName.get(method.getReturnType())).addJavadoc(bundle.comment());

      if (bundle.parserClass().isPresent()) {
        TypeMirror parser = bundle.parserClass().get();
        builder.addStatement("$T parser = new $T()", parser, parser);
        builder.addStatement(String.format("return parser.fromString(config.getString(\"%s\"))",
            prefix + bundle.key()));
      } else {
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();
        TypeMirror returnType = method.getReturnType();
        if (types.isAssignable(returnType, elements.getTypeElement("java.lang.Boolean").asType())) {
          builder.addStatement(
              String.format("return config.getBoolean(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Byte").asType())) {
          builder
              .addStatement(String.format("return config.getByte(\"%s\")", prefix + bundle.key()));

        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Double").asType())) {
          builder.addStatement(
              String.format("return config.getDouble(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Float").asType())) {
          builder
              .addStatement(String.format("return config.getFloat(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Integer").asType())) {
          builder
              .addStatement(String.format("return config.getInt(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Long").asType())) {
          builder
              .addStatement(String.format("return config.getLong(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.Short").asType())) {
          builder
              .addStatement(String.format("return config.getShort(\"%s\")", prefix + bundle.key()));
        } else if (types.isAssignable(returnType,
            elements.getTypeElement("java.lang.String").asType())) {
          builder.addStatement(
              String.format("return config.getString(\"%s\")", prefix + bundle.key()));
        } else {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
              "Can't handle return type " + m.getReturnType().getCanonicalName());
        }
      }
      typeBuilder.addMethod(builder.build());
    }
    methodBuilder.addStatement("return $L", typeBuilder.build());

    return methodBuilder.build();
  }

}
