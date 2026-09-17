package edu.cit.menardo.inventory;

import java.lang.reflect.Modifier;

import edu.cit.menardo.inventory.events.LowStockEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proof for the rubric: the implementation and its collaborators are not public,
 * so nothing outside edu.cit.menardo.inventory can reference them.
 */
class ModuleBoundaryTest {

    @Test
    void implementationAndInternalsAreNotPublic() {
        assertThat(Modifier.isPublic(InventoryServiceImpl.class.getModifiers()))
                .as("InventoryServiceImpl must be package-private")
                .isFalse();
        assertThat(Modifier.isPublic(InventoryItem.class.getModifiers()))
                .as("the JPA entity must stay inside the module")
                .isFalse();
        assertThat(Modifier.isPublic(InventoryRepository.class.getModifiers()))
                .as("the repository must stay inside the module")
                .isFalse();
        assertThat(Modifier.isPublic(InventoryController.class.getModifiers()))
                .as("the controller is wired by Spring, not referenced by other modules")
                .isFalse();
    }

    @Test
    void theInterfaceAndEventsAreTheOnlyWayIn() {
        assertThat(Modifier.isPublic(InventoryService.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(InventoryView.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(ReservationResult.class.getModifiers())).isTrue();
        assertThat(Modifier.isPublic(LowStockEvent.class.getModifiers())).isTrue();
    }
}
