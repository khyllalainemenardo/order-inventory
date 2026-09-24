package edu.cit.menardo.supplier;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

final class LegacySupplyXml {

    private LegacySupplyXml() {
    }

    static String authRequest(String clientId, String apiKey) {
        return "<AuthRequest><ClientId>" + escape(clientId) + "</ClientId>"
                + "<ApiKey>" + escape(apiKey) + "</ApiKey></AuthRequest>";
    }

    static String purchaseOrder(String supplierSku, int qty, String buyerRef) {
        return "<PurchaseOrder><SupplierSku>" + escape(supplierSku) + "</SupplierSku>"
                + "<Qty>" + qty + "</Qty>"
                + "<BuyerRef>" + escape(buyerRef) + "</BuyerRef></PurchaseOrder>";
    }

    static String sessionToken(String xml) {
        return text(parse(xml).getDocumentElement(), "SessionToken");
    }

    static String errorCode(String xml) {
        try {
            return text(parse(xml).getDocumentElement(), "Code");
        } catch (RuntimeException notAnLsError) {
            return "UNKNOWN";
        }
    }

    static LegacyOrderAck orderAck(String xml) {
        return toAck(parse(xml).getDocumentElement());
    }

    static Optional<LegacyOrderAck> firstOrderInList(String xml) {
        Node poNumber = parse(xml).getElementsByTagName("PoNumber").item(0);
        if (poNumber == null) {
            return Optional.empty();
        }
        return Optional.of(toAck((Element) poNumber.getParentNode()));
    }

    private static LegacyOrderAck toAck(Element order) {
        return new LegacyOrderAck(
                text(order, "PoNumber"),
                Integer.parseInt(text(order, "StatusCode")),
                Integer.parseInt(text(order, "Qty")),
                text(order, "Uom"));
    }

    private static String text(Element parent, String tag) {
        Node node = parent.getElementsByTagName(tag).item(0);
        if (node == null) {
            throw new IllegalArgumentException("LegacySupply document has no <" + tag + ">");
        }
        return node.getTextContent().trim();
    }

    private static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalArgumentException("LegacySupply sent a document that is not valid XML", e);
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
