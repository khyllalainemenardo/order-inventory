package edu.cit.menardo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


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
                        .matches("import edu\\.cit\\.menardo\\.(shop|inventory|supplier)\\.events\\.\\w+;");
            }
            assertThat(source).as(file.toString())
                    .doesNotContain("OrderService")
                    .doesNotContain("InventoryService");
        }
    }

    @Test
    void orderAndInventoryKnowNothingAboutLegacySupply() throws IOException {
        for (String module : List.of("shop", "inventory")) {
            for (Path file : javaFiles(module)) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString())
                        .doesNotContain("LegacySupply")
                        .doesNotContain("SupplierSku")
                        .doesNotContain("PackSize")
                        .doesNotContain("Uom")
                        .doesNotContain("StatusCode")
                        .doesNotContain("WLU-");
                for (String line : source.lines().filter(l -> l.startsWith("import edu.cit.menardo.supplier")).toList()) {
                    assertThat(line)
                            .as(file + " may only use the supplier gateway, its result types and events")
                            .matches("import edu\\.cit\\.menardo\\.supplier\\.(SupplierGateway|ReorderResult|SupplierOrderStatus|events\\.\\w+);");
                }
            }
        }
    }

    @Test
    void orderAndInventoryDoNotKnowTianggeExists() throws IOException {
        for (String module : List.of("shop", "inventory")) {
            for (Path file : javaFiles(module)) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString())
                        .doesNotContainIgnoringCase("tiangge")
                        .doesNotContain("edu.cit.menardo.channel")
                        .doesNotContain("sellerSku");
            }
        }
    }

    private static List<Path> javaFiles(String module) throws IOException {
        try (Stream<Path> paths = Files.walk(ROOT.resolve(module))) {
            return paths.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
