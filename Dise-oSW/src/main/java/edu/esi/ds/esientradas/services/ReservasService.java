package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.TokenDao;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Token;

@Service
public class ReservasService {
    private static final long RESERVA_TTL_MILLIS = 10 * 60 * 1000;

    @Autowired
    private EntradaDao dao;

    @Autowired
    private TokenDao tokenDao;

    @Transactional
    public Long reservar(Long idEntrada, String sesionId) {
        Entrada entrada = this.dao.findById(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        List<Token> tokensEntrada = this.tokenDao.findAllByEntradaId(idEntrada);

        for (Token tokenExistente : tokensEntrada) {
            if (
                tokenExistente.getSesionId() != null
                && tokenExistente.getSesionId().equals(sesionId)
                && entrada.getEstado() == Estado.RESERVADA
            ) {
                tokenExistente.setHoraActiva(System.currentTimeMillis());
                this.tokenDao.saveAndFlush(tokenExistente);
                return entrada.getPrecio();
            }
        }

        int updated = this.dao.updateEstadoIf(
            idEntrada,
            Estado.RESERVADA.name(),
            Estado.DISPONIBLE.name()
        );

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible para reservar");
        }

        entrada = this.dao.findById(idEntrada)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        Token token = tokensEntrada.isEmpty() ? new Token() : tokensEntrada.get(0);
        token.setEntrada(entrada);
        token.setSesionId(sesionId);
        token.setHoraActiva(System.currentTimeMillis());

        try {
            this.tokenDao.saveAndFlush(token);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La entrada no está disponible para reservar");
        }

        return entrada.getPrecio();
    }

    @Transactional
    public Long reservarEntradas(List<Long> entradaIds, String sesionId) {
        if (entradaIds == null || entradaIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes seleccionar al menos una entrada");
        }

        long precioTotal = 0L;
        for (Long idEntrada : entradaIds) {
            precioTotal += this.reservar(idEntrada, sesionId);
        }
        return precioTotal;
    }

    public DtoReservaResponse getResumen(String sesionId) {
        List<Token> tokens = this.tokenDao.findBySesionId(sesionId);
        long precioTotal = tokens.stream().mapToLong(t -> t.getEntrada().getPrecio()).sum();
        return new DtoReservaResponse(precioTotal, tokens.size());
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void liberarReservasCaducadas() {
        long horaLimite = System.currentTimeMillis() - RESERVA_TTL_MILLIS;
        List<Token> tokensCaducados = this.tokenDao.findExpiredWithEntrada(horaLimite);
        List<Entrada> reservasHuerfanas = this.dao.findByEstadoWithoutToken(Estado.RESERVADA);

        for (Token token : tokensCaducados) {
            Entrada entrada = token.getEntrada();
            if (entrada != null && entrada.getEstado() == Estado.RESERVADA) {
                this.dao.updateEstadoIf(
                    entrada.getId(),
                    Estado.DISPONIBLE.name(),
                    Estado.RESERVADA.name()
                );
            }
        }

        if (!tokensCaducados.isEmpty()) {
            this.tokenDao.deleteAll(tokensCaducados);
        }

        for (Entrada entrada : reservasHuerfanas) {
            this.dao.updateEstadoIf(
                entrada.getId(),
                Estado.DISPONIBLE.name(),
                Estado.RESERVADA.name()
            );
        }
    }
}
