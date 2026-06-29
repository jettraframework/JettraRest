package io.jettra.rest.client.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.auto.service.AutoService;
import javax.annotation.processing.Processor;

@AutoService(Processor.class)
@SupportedAnnotationTypes("io.jettra.rest.client.RestClient")
public class RestClientProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.INTERFACE) {
                    processInterface((TypeElement) element);
                }
            }
        }
        return true;
    }

    private void processInterface(TypeElement interfaceElement) {
        String interfaceName = interfaceElement.getSimpleName().toString();
        PackageElement packageElement = (PackageElement) interfaceElement.getEnclosingElement();
        String originalPackage = packageElement.getQualifiedName().toString();

        String targetPackage = originalPackage;
        if (targetPackage.endsWith(".interfaces")) {
            targetPackage = targetPackage.substring(0, targetPackage.length() - ".interfaces".length());
        }

        String targetClassName = interfaceName;
        if (targetClassName.startsWith("I") && targetClassName.length() > 1 && Character.isUpperCase(targetClassName.charAt(1))) {
            targetClassName = targetClassName.substring(1);
        } else {
            targetClassName = targetClassName + "Impl";
        }

        String qualifiedTargetClassName = targetPackage + "." + targetClassName;

        try {
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(qualifiedTargetClassName);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                out.println("package " + targetPackage + ";");
                out.println();
                out.println("import " + interfaceElement.getQualifiedName().toString() + ";");
                out.println("import io.jettra.rest.client.RestClientBuilder;");
                out.println();
                out.println("public class " + targetClassName + " implements " + interfaceName + " {");
                out.println();
                out.println("    private final " + interfaceName + " proxy = RestClientBuilder.create(" + interfaceName + ".class);");
                out.println();

                for (Element enclosed : interfaceElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.METHOD) {
                        ExecutableElement method = (ExecutableElement) enclosed;
                        String methodName = method.getSimpleName().toString();
                        String returnType = method.getReturnType().toString();
                        
                        String params = method.getParameters().stream()
                                .map(p -> p.asType().toString() + " " + p.getSimpleName().toString())
                                .collect(Collectors.joining(", "));
                        
                        String args = method.getParameters().stream()
                                .map(p -> p.getSimpleName().toString())
                                .collect(Collectors.joining(", "));

                        out.println("    @Override");
                        out.println("    public " + returnType + " " + methodName + "(" + params + ") {");
                        if ("void".equals(returnType)) {
                            out.println("        proxy." + methodName + "(" + args + ");");
                        } else {
                            out.println("        return proxy." + methodName + "(" + args + ");");
                        }
                        out.println("    }");
                        out.println();
                    }
                }
                out.println("}");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate class: " + e.getMessage(), interfaceElement);
        }
    }
}
