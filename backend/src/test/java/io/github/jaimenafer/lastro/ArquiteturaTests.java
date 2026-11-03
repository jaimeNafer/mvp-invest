package io.github.jaimenafer.lastro;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class ArquiteturaTests {
  @Test
  void contratosArquiteturaisSaoRespeitados() throws Exception {
    var violacoes = new ArrayList<String>();
    try (var arquivos = Files.walk(Path.of("src/main/java/io/github/jaimenafer/lastro"))) {
      for (var arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
        var texto = Files.readString(arquivo);
        var pacote = texto.substring(texto.indexOf("package ") + 8, texto.indexOf(';'));
        if (pacote.contains(".domain.")
            && (texto.contains("import org.springframework.")
                || texto.contains(".application.")
                || texto.contains(".infrastructure.")
                || texto.contains(".api."))) violacoes.add(arquivo + ": domínio depende de Spring");
        if (texto
            .lines()
            .anyMatch(l ->
                l.matches(
                    "    (public|private|protected) (static )?(record|class|interface|enum) .*")))
          violacoes.add(arquivo + ": tipo aninhado");
        if (texto.contains("@RestController") && !texto.contains("@RestControllerAdvice")) {
          if (!pacote.endsWith(".api.controller"))
            violacoes.add(arquivo + ": controller fora da API");
          if (texto.contains("Repository")
              || texto.contains("@ExceptionHandler")
              || texto.contains(".stream()"))
            violacoes.add(arquivo + ": controller mistura responsabilidades");
        }
        if (pacote.contains(".application.") && texto.contains(".infrastructure."))
          violacoes.add(arquivo + ": aplicação depende de infraestrutura");
      }
    }
    assertEquals(List.of(), violacoes);
  }

  @Test
  void controllersDependemDeInterfacesEDtosNaoExpoemEntidades() throws Exception {
    try (var arquivos = Files.walk(Path.of("src/main/java/io/github/jaimenafer/lastro"))) {
      for (var arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
        var texto = Files.readString(arquivo);
        var pacote = texto.substring(texto.indexOf("package ") + 8, texto.indexOf(';'));
        var nome = arquivo.getFileName().toString().replace(".java", "");
        var tipo = Class.forName(pacote + "." + nome, false, getClass().getClassLoader());
        assertEquals(0, tipo.getDeclaredClasses().length, tipo.getName());
        if (pacote.endsWith(".api.controller")) {
          for (var campo : tipo.getDeclaredFields()) {
            assertTrue(campo.getType().isInterface(), campo.toString());
            assertTrue(
                campo.getType().getPackageName().endsWith(".application.usecase"),
                campo.toString());
          }
        }
        if (pacote.endsWith(".application.usecase")) {
          assertTrue(tipo.isInterface(), tipo.getName());
          for (var metodo : tipo.getDeclaredMethods()) {
            validarAusenciaDeEntidade(metodo.getGenericReturnType(), new HashSet<>());
            for (var parametro : metodo.getGenericParameterTypes())
              validarAusenciaDeEntidade(parametro, new HashSet<>());
          }
        }
        if (pacote.endsWith(".application.usecase.impl")
            && tipo.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
          for (var construtor : tipo.getConstructors())
            for (var parametro : construtor.getParameterTypes())
              assertTrue(parametro.isInterface(), construtor.toString());
        }
      }
    }
  }

  @Test
  void interfacesTemPrefixoIEModelosNaoExpoemSetters() throws Exception {
    try (var arquivos = Files.walk(Path.of("src/main/java/io/github/jaimenafer/lastro"))) {
      for (var arquivo : arquivos.filter(p -> p.toString().endsWith(".java")).toList()) {
        var texto = Files.readString(arquivo);
        var pacote = texto.substring(texto.indexOf("package ") + 8, texto.indexOf(';'));
        var nome = arquivo.getFileName().toString().replace(".java", "");
        var tipo = Class.forName(pacote + "." + nome, false, getClass().getClassLoader());
        if (tipo.isInterface()) assertTrue(nome.matches("I[A-Z].*"), nome);
        if (tipo.isAnnotationPresent(jakarta.persistence.Entity.class))
          for (var metodo : tipo.getDeclaredMethods())
            assertFalse(
                metodo.getName().startsWith("set") && metodo.getParameterCount() == 1,
                metodo.toString());
      }
    }
  }

  @Test
  void annotationsUtilizamNomesSimplesComImports() throws Exception {
    var compilador = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compilador, "A verificação arquitetural exige o JDK do projeto.");
    var violacoes = new ArrayList<String>();
    try (var fontes = Files.walk(Path.of("src"));
        var gerenciador = compilador.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
      var arquivos = fontes.filter(p -> p.toString().endsWith(".java")).toList();
      var tarefa =
          (JavacTask)
              compilador.getTask(
                  null,
                  gerenciador,
                  null,
                  List.of("-proc:none"),
                  null,
                  gerenciador.getJavaFileObjectsFromPaths(arquivos));
      var arvores = Trees.instance(tarefa);
      for (var unidade : tarefa.parse()) {
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitAnnotation(AnnotationTree annotation, Void ignorado) {
            if (annotation.getAnnotationType().toString().contains(".")) {
              long posicao = arvores.getSourcePositions().getStartPosition(annotation);
              violacoes.add(
                  unidade.getSourceFile().getName()
                      + ":"
                      + unidade.getLineMap().getLineNumber(posicao)
                      + " — importar "
                      + annotation.getAnnotationType()
                      + " e usar o nome simples");
            }
            return super.visitAnnotation(annotation, ignorado);
          }
        }.scan(unidade, null);
      }
    }
    assertEquals(List.of(), violacoes);
  }

  private void validarAusenciaDeEntidade(
      java.lang.reflect.Type tipo, Set<java.lang.reflect.Type> visitados) {
    if (!visitados.add(tipo)) return;
    if (tipo instanceof java.lang.reflect.ParameterizedType generico) {
      validarAusenciaDeEntidade(generico.getRawType(), visitados);
      for (var argumento : generico.getActualTypeArguments())
        validarAusenciaDeEntidade(argumento, visitados);
    }
    if (tipo instanceof Class<?> classe) {
      assertFalse(classe.isAnnotationPresent(jakarta.persistence.Entity.class), classe.getName());
      if (classe.isRecord())
        for (var campo : classe.getRecordComponents())
          validarAusenciaDeEntidade(campo.getGenericType(), visitados);
    }
  }
}
