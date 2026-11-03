package io.github.jaimenafer.lastro;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ArquiteturaModularTests {
  private static final String BASE = "io.github.jaimenafer.lastro";
  private static final Map<String, Set<String>> PERMITIDAS =
      Map.of(
          "acesso",
          Set.of(),
          "ativos",
          Set.of(),
          "instituicoes",
          Set.of(),
          "carteiras",
          Set.of("acesso", "instituicoes"),
          "operacoes",
          Set.of("acesso", "ativos", "instituicoes", "carteiras"));
  private static final JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages(BASE);

  @Test
  void contextosComunicamSomentePorContratosPublicosAutorizados() {
    classes()
        .should(
            new ArchCondition<JavaClass>(
                "usar somente contratos públicos autorizados entre contextos") {
              @Override
              public void check(JavaClass origem, ConditionEvents eventos) {
                String contexto = contexto(origem);
                if (contexto.isEmpty() || Set.of("exceptions", "config").contains(contexto)) return;
                if (!PERMITIDAS.containsKey(contexto)) {
                  eventos.add(
                      SimpleConditionEvent.violated(
                          origem, "Contexto não registrado na matriz: " + contexto));
                  return;
                }
                for (var dependencia : origem.getDirectDependenciesFromSelf()) {
                  var destino = dependencia.getTargetClass();
                  String outro = contexto(destino);
                  if (outro.isEmpty() || outro.equals(contexto)) continue;
                  boolean autorizado =
                      PERMITIDAS.get(contexto).contains(outro)
                          && origem.getPackageName().contains(".application.")
                          && destino
                              .getPackageName()
                              .equals(BASE + "." + outro + ".application.contract");
                  if (!autorizado)
                    eventos.add(
                        SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                }
              }
            })
        .check(CLASSES);
  }

  @Test
  void contextosNaoPossuemCiclos() {
    slices().matching(BASE + ".(*)..").should().beFreeOfCycles().check(CLASSES);
  }

  @Test
  void camadasRespeitamADirecaoDasDependencias() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..application..", "..infrastructure..", "..api..", BASE + ".exceptions..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..", "..api..", BASE + ".exceptions..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage("..api..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..domain..",
            "..infrastructure..",
            "..application.mapper..",
            "..application.usecase.impl..")
        .check(CLASSES);
    noClasses()
        .that()
        .resideInAPackage("..infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..api..")
        .check(CLASSES);
  }

  @Test
  void dominioAdmiteSomenteAExcecaoJpaDocumentada() {
    classes()
        .that()
        .resideInAPackage("..domain..")
        .should(
            new ArchCondition<JavaClass>(
                "ser independente de frameworks exceto mapeamento JPA nos modelos") {
              @Override
              public void check(JavaClass origem, ConditionEvents eventos) {
                for (var dependencia : origem.getDirectDependenciesFromSelf()) {
                  var destino = dependencia.getTargetClass();
                  if (destino.isArray()) destino = destino.getBaseComponentType();
                  String nome = destino.getName();
                  boolean modelo =
                      origem.getPackageName().endsWith(".domain.model")
                          || origem.getPackageName().contains(".domain.model.");
                  boolean jpa =
                      nome.startsWith("jakarta.persistence.")
                          && (destino.isAnnotation() || destino.isEnum());
                  boolean hibernate =
                      Set.of(
                              "org.hibernate.annotations.JdbcTypeCode",
                              "org.hibernate.type.SqlTypes")
                          .contains(nome);
                  boolean permitido =
                      destino.isPrimitive()
                          || nome.startsWith("java.")
                          || nome.startsWith("lombok.")
                          || nome.startsWith(BASE + "." + contexto(origem) + ".domain.")
                          || (modelo && (jpa || hibernate));
                  if (!permitido)
                    eventos.add(
                        SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                }
              }
            })
        .check(CLASSES);
  }

  @Test
  void contratosPublicosNaoVazamModelosOuFrameworks() {
    noClasses()
        .that()
        .resideInAPackage("..application.contract..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..domain..",
            "..infrastructure..",
            "..api..",
            "..application.dto..",
            "org.springframework..",
            "jakarta.persistence..",
            "org.hibernate..",
            "tools.jackson..")
        .check(CLASSES);
  }

  @Test
  void tratamentoTransversalAcessaSomenteExcecoesDeNegocio() {
    classes()
        .that()
        .resideInAPackage(BASE + ".exceptions..")
        .should(
            new ArchCondition<JavaClass>("não acessar implementações dos contextos") {
              @Override
              public void check(JavaClass origem, ConditionEvents eventos) {
                for (var dependencia : origem.getDirectDependenciesFromSelf()) {
                  var destino = dependencia.getTargetClass();
                  if (PERMITIDAS.containsKey(contexto(destino))
                      && !destino.getPackageName().contains(".domain.exception"))
                    eventos.add(
                        SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                }
              }
            })
        .check(CLASSES);
  }

  @Test
  void contratosPublicosNaoDependemDeInternosDoProprioContexto() {
    classes()
        .that()
        .resideInAPackage("..application.contract..")
        .should(
            new ArchCondition<JavaClass>("não depender de nenhum pacote interno") {
              @Override
              public void check(JavaClass origem, ConditionEvents eventos) {
                for (var dependencia : origem.getDirectDependenciesFromSelf()) {
                  var destino = dependencia.getTargetClass();
                  if (destino.getPackageName().startsWith(BASE + ".")
                      && !destino.getPackageName().endsWith(".application.contract"))
                    eventos.add(
                        SimpleConditionEvent.violated(dependencia, dependencia.getDescription()));
                }
              }
            })
        .check(CLASSES);
  }

  @Test
  void handlerHttpNaoConhecePersistencia() {
    noClasses()
        .that()
        .resideInAPackage(BASE + ".exceptions..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("org.hibernate..", "org.springframework.dao..", "java.sql..")
        .check(CLASSES);
  }

  private static String contexto(JavaClass tipo) {
    String pacote = tipo.getPackageName();
    if (!pacote.startsWith(BASE + ".")) return "";
    return pacote.substring(BASE.length() + 1).split("\\.")[0];
  }
}
