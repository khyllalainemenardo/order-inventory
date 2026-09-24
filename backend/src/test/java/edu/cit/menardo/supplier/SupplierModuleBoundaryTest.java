package edu.cit.menardo.supplier;

import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierModuleBoundaryTest {

    @Test
    void onlyTheGatewayAndOurOwnTypesArePublic() {
        List<Class<?>> publicTypes = List.of(SupplierGateway.class, ReorderResult.class, SupplierOrderStatus.class);
        List<Class<?>> internalTypes = List.of(
                ReorderService.class, ReorderSender.class, DeliveryTracker.class, SupplierJobs.class,
                LegacySupplyClient.class, LegacySupplyXml.class, LegacySupplyTranslator.class,
                LegacyOrderAck.class, LegacySupplyException.class,
                SupplierOrder.class, SupplierOrderRepository.class, SupplierOrderController.class);

        publicTypes.forEach(type -> assertThat(Modifier.isPublic(type.getModifiers())).as(type.getSimpleName()).isTrue());
        internalTypes.forEach(type -> assertThat(Modifier.isPublic(type.getModifiers())).as(type.getSimpleName()).isFalse());
    }
}
