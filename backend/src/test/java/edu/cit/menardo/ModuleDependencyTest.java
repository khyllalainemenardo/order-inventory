package edu.cit.menardo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Lab 2 dependency rule, checked against the source files:
 *
 *   - shop and inventory never import anything from notification;
 *   - notification imports nothing from shop or inventory except their events
 *     packages, and never mentions OrderService or InventoryService.
 *
 * Maven and IntelliJ both run tests with the backend folder as working directory.
 */
class ModuleDependencyTest {

    private static final Path ROOT = Path.of("src/main/java/edu/cit/menardo");

    @Test
    void orderAndInventoryDoNotKnowAboutNotifications() throws IOException {
        for (String module : List.of("shop", "inventory")) {
            for (Path file : javaFiles(module)) {
                assertThat(Files.readString(file))
                        .as(file.toString())
                        .doesNotContain("edu.cit.menardo.notification");
            }
        }
    }

    @Test
    void notificationsDependOnEventsOnly() throws IOException {
        List<Path> files = javaFiles("notification");
        assertThat(files).isNotEmpty();
        for (Path file : files) {
            String source = Files.readString(file);
            for (String line : source.lines().filter(l -> l.startsWith("import edu.cit.menardo.")).toList()) {
                assertThat(line)
                        .as(file + " may only import event classes")
                        .matches("import edu\\.cit\\.menardo\\.(shop|inventory)\\.events\\.\\w+;");
            }
            assertThat(source).as(file.toString())
                    .doesNotContain("OrderService")
                    .doesNotContain("InventoryService");
        }
    }

    private static List<Path> javaFiles(String module) throws IOException {
        try (Stream<Path> paths = Files.walk(ROOT.resolve(module))) {
            return paths.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
