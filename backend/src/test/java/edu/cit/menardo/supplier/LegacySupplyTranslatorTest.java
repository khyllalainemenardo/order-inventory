package edu.cit.menardo.supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacySupplyTranslatorTest {

    private final LegacySupplyTranslator translator = new LegacySupplyTranslator();

    @Test
    void roundsUnitsUpToWholeCases() {
        assertThat(translator.casesFor("P100", 16)).isEqualTo(1);
        assertThat(translator.casesFor("P100", 24)).isEqualTo(1);
        assertThat(translator.casesFor("P100", 25)).isEqualTo(2);
        assertThat(translator.casesFor("P300", 20)).isEqualTo(2);
        assertThat(translator.unitsIn("P100", 1)).isEqualTo(24);
        assertThat(translator.unitsIn("P300", 2)).isEqualTo(20);
    }

    @Test
    void neverOrdersMoreThanTheSupplierAllows() {
        assertThat(translator.casesFor("P300", 5000)).isEqualTo(99);
    }

    @Test
    void mapsKnownStatusCodesAndFlagsUnknownOnes() {
        assertThat(translator.toStatus(10)).isEqualTo(SupplierOrderStatus.PLACED);
        assertThat(translator.toStatus(20)).isEqualTo(SupplierOrderStatus.PICKING);
        assertThat(translator.toStatus(30)).isEqualTo(SupplierOrderStatus.SHIPPED);
        assertThat(translator.toStatus(40)).isEqualTo(SupplierOrderStatus.DELIVERED);
        assertThat(translator.toStatus(90)).isEqualTo(SupplierOrderStatus.NEEDS_REVIEW);
    }

    @Test
    void readsLegacySupplyDocuments() {
        LegacyOrderAck ack = LegacySupplyXml.orderAck("""
                <PurchaseOrderAck><PoNumber>PO-100231</PoNumber><StatusCode>10</StatusCode>
                <SupplierSku>WLU-6392</SupplierSku><Qty>1</Qty><Uom>CS</Uom><BuyerRef>RO-1</BuyerRef></PurchaseOrderAck>""");

        assertThat(ack).isEqualTo(new LegacyOrderAck("PO-100231", 10, 1, "CS"));
        assertThat(LegacySupplyXml.firstOrderInList("<PurchaseOrderList><Count>0</Count></PurchaseOrderList>")).isEmpty();
        assertThat(LegacySupplyXml.errorCode("<LSError><Code>E-AUTH-07</Code><Message>x</Message></LSError>")).isEqualTo("E-AUTH-07");
    }
}
