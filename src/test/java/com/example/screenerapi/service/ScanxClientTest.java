package com.example.screenerapi.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanxClientTest {
    @Test
    void parsesIsinUsingHeadersRatherThanAssumedPositions() {
        String response = "{\"code\":0,\"headers\":[\"Sym\",\"DispSym\",\"Isin\",\"Volume\"],"
                + "\"data\":[[\"ABC\",\"Example\",\"INE123\",12345]]}";

        List<ScanxStock> stocks = ScanxClient.parse(response);

        assertEquals(1, stocks.size());
        assertEquals("INE123", stocks.get(0).getIsin());
        assertEquals("ABC", stocks.get(0).getSymbol());
        assertEquals(12345, stocks.get(0).getValues().get("Volume"));
    }

    @Test
    void skipsRowsWithoutAnIsin() {
        String response = "{\"code\":0,\"headers\":[\"Isin\",\"Sym\"],"
                + "\"data\":[[null,\"ABC\"]]}";

        assertTrue(ScanxClient.parse(response).isEmpty());
    }
}