package edu.esi.ds.esientradas.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.PaymentIntent;

import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Token;

class PagosServiceTest {

    private final PagosService pagosService = new PagosService();

    @Test
    void rechazaPagoConImporteDistintoAlCalculadoEnBackend() {
        PaymentIntent paymentIntent = paymentIntent(3_500L, "eur", "10,11");
        List<Token> reservas = List.of(token(10L, 3_500L), token(11L, 3_500L));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> pagosService.validarPagoCoincideConReservas(paymentIntent, reservas));

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void rechazaPagoConEntradasDistintasALasReservadas() {
        PaymentIntent paymentIntent = paymentIntent(7_000L, "eur", "10,99");
        List<Token> reservas = List.of(token(10L, 3_500L), token(11L, 3_500L));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> pagosService.validarPagoCoincideConReservas(paymentIntent, reservas));

        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void aceptaPagoConMismasEntradasEImporteAunqueCambieElOrden() {
        PaymentIntent paymentIntent = paymentIntent(7_000L, "EUR", "11,10");
        List<Token> reservas = List.of(token(10L, 3_500L), token(11L, 3_500L));

        assertDoesNotThrow(() -> pagosService.validarPagoCoincideConReservas(paymentIntent, reservas));
    }

    private PaymentIntent paymentIntent(Long amount, String currency, String entryIds) {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getAmount()).thenReturn(amount);
        when(paymentIntent.getCurrency()).thenReturn(currency);
        when(paymentIntent.getMetadata()).thenReturn(Map.of("entryIds", entryIds));
        return paymentIntent;
    }

    private Token token(Long entradaId, Long precio) {
        DeZona entrada = new DeZona();
        entrada.setId(entradaId);
        entrada.setPrecio(precio);

        Token token = new Token();
        token.setEntrada(entrada);
        return token;
    }
}
