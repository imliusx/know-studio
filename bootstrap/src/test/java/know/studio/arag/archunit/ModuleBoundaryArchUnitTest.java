package know.studio.arag.archunit;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ModuleBoundaryArchUnitTest {

    private static final String ROOT_PACKAGE = "know.studio.arag";
    private static final List<String> BUSINESS_MODULES = List.of(
            "identity",
            "knowledge",
            "retrieval",
            "agent",
            "conversation",
            "evaluation"
    );

    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter().importPackages(ROOT_PACKAGE);

    @Test
    void platformModulesMustNotDependOnBusinessModules() {
        ArchRule rule = classes()
                .that().resideInAPackage(ROOT_PACKAGE + ".platform..")
                .should(notDependOnBusinessModules());

        rule.check(classes);
    }

    @Test
    void businessModulesMayOnlyUseAnotherModulesApiPackage() {
        ArchRule rule = classes()
                .that().resideInAnyPackage(businessPackagePatterns())
                .should(onlyUseOtherBusinessModulesThroughApi());

        rule.check(classes);
    }

    private static ArchCondition<JavaClass> notDependOnBusinessModules() {
        return new ArchCondition<>("not depend on business modules") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> businessModule(dependency.getTargetClass().getPackageName()) != null)
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(item, dependency.getDescription())));
            }
        };
    }

    private static ArchCondition<JavaClass> onlyUseOtherBusinessModulesThroughApi() {
        return new ArchCondition<>("only use other business modules through their api packages") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String originModule = businessModule(item.getPackageName());
                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    String targetModule = businessModule(targetPackage);
                    if (targetModule != null
                            && !targetModule.equals(originModule)
                            && !targetPackage.startsWith(ROOT_PACKAGE + "." + targetModule + ".api")) {
                        events.add(SimpleConditionEvent.violated(item, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static String[] businessPackagePatterns() {
        return BUSINESS_MODULES.stream()
                .map(module -> ROOT_PACKAGE + "." + module + "..")
                .toArray(String[]::new);
    }

    private static String businessModule(String packageName) {
        return BUSINESS_MODULES.stream()
                .filter(module -> packageName.equals(ROOT_PACKAGE + "." + module)
                        || packageName.startsWith(ROOT_PACKAGE + "." + module + "."))
                .findFirst()
                .orElse(null);
    }
}
