package edu.cit.menardo.inventory;

import java.util.Optional;

import edu.cit.menardo.inventory.events.LowStockEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The low-stock rule and restock, with the repository mocked. */
class InventoryServiceImplTest {

    private final InventoryRepository repository = mock(InventoryRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final InventoryServiceImpl service = new InventoryServiceImpl(repository, events, 5);

    @Test
    void publishesLowStockWhenAReservationDropsBelowTheThreshold() {
        when(repository.findById("P200")).thenReturn(
                Optional.of(new InventoryItem("P200", "Mechanical Keyboard", 10)),
                Optional.of(new InventoryItem("P200", "Mechanical Keyboard", 4)));
        when(repository.deductIfAvailable("P200", 6)).thenReturn(1);

        ReservationResult result = service.reserve("P200", 6);

        assertThat(result.confirmed()).isTrue();
        assertThat(result.inventory().lowStock()).isTrue();
        verify(events).publishEvent(new LowStockEvent("P200", "Mechanical Keyboard", 4, 5));
    }

    @Test
    void staysQuietWhenStockIsStillAtOrAboveTheThreshold() {
        when(repository.findById("P100")).thenReturn(
                Optional.of(new InventoryItem("P100", "Wireless Mouse", 25)),
                Optional.of(new InventoryItem("P100", "Wireless Mouse", 5)));
        when(repository.deductIfAvailable("P100", 20)).thenReturn(1);

        service.reserve("P100", 20);

        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void noAlertForAFailedReservation() {
        when(repository.findById("P300")).thenReturn(
                Optional.of(new InventoryItem("P300", "USB-C Hub", 0)));
        when(repository.deductIfAvailable("P300", 1)).thenReturn(0);

        ReservationResult result = service.reserve("P300", 1);

        assertThat(result.confirmed()).isFalse();
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void restockAddsStockBack() {
        when(repository.addStock("P200", 6)).thenReturn(1);
        when(repository.findById("P200")).thenReturn(
                Optional.of(new InventoryItem("P200", "Mechanical Keyboard", 10)));

        InventoryView view = service.restock("P200", 6);

        assertThat(view.stock()).isEqualTo(10);
        verify(repository).addStock(eq("P200"), eq(6));
    }

    @Test
    void restockRejectsUnknownProductsAndBadQuantities() {
        when(repository.addStock("P999", 1)).thenReturn(0);

        assertThatThrownBy(() -> service.restock("P999", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.restock("P100", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
