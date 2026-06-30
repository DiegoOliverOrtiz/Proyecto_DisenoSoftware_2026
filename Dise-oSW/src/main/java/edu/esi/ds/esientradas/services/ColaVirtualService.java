package edu.esi.ds.esientradas.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoColaEstado;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;

@Service
public class ColaVirtualService {
    private static final long TURNO_TTL_MILLIS = Duration.ofMinutes(10).toMillis();

    private final EspectaculoDao espectaculoDao;
    private final EntradaDao entradaDao;
    private final Map<Long, ArrayDeque<ColaEntry>> colas = new HashMap<>();
    private final Map<Long, TurnoActivo> turnos = new HashMap<>();

    public ColaVirtualService(EspectaculoDao espectaculoDao, EntradaDao entradaDao) {
        this.espectaculoDao = espectaculoDao;
        this.entradaDao = entradaDao;
    }

    public synchronized DtoColaEstado join(Long espectaculoId, String sesionId) {
        Espectaculo espectaculo = requireEspectaculo(espectaculoId);
        if (!requiresQueue(espectaculo)) {
            return freeAccess(espectaculo);
        }
        if (!isOpen(espectaculo)) {
            DtoColaEstado estado = base(espectaculo);
            estado.setMessage("La taquilla virtual todavia no esta abierta.");
            return estado;
        }
        if (isSoldOut(espectaculoId)) {
            leave(espectaculoId, sesionId);
            return soldOut(espectaculo);
        }

        cleanupExpiredTurn(espectaculoId);
        TurnoActivo turno = turnos.get(espectaculoId);
        if (turno != null && turno.sesionId.equals(sesionId)) {
            return activeTurn(espectaculo, turno);
        }

        ArrayDeque<ColaEntry> cola = colas.computeIfAbsent(espectaculoId, ignored -> new ArrayDeque<>());
        if (cola.stream().noneMatch(entry -> entry.sesionId.equals(sesionId))) {
            cola.addLast(new ColaEntry(sesionId));
        }
        promoteIfPossible(espectaculoId);
        return status(espectaculoId, sesionId);
    }

    public synchronized DtoColaEstado status(Long espectaculoId, String sesionId) {
        Espectaculo espectaculo = requireEspectaculo(espectaculoId);
        if (!requiresQueue(espectaculo)) {
            return freeAccess(espectaculo);
        }
        cleanupExpiredTurn(espectaculoId);
        if (isOpen(espectaculo)) {
            promoteIfPossible(espectaculoId);
        }
        if (isSoldOut(espectaculoId)) {
            leave(espectaculoId, sesionId);
            return soldOut(espectaculo);
        }

        TurnoActivo turno = turnos.get(espectaculoId);
        if (turno != null && turno.sesionId.equals(sesionId)) {
            return activeTurn(espectaculo, turno);
        }

        DtoColaEstado estado = base(espectaculo);
        ArrayDeque<ColaEntry> cola = colas.get(espectaculoId);
        int posicion = positionOf(cola, sesionId);
        estado.setEnCola(posicion > 0);
        estado.setPosicion(posicion);
        estado.setPersonasDelante(posicion > 0 ? posicion - 1 : 0);
        estado.setMessage(posicion > 0 ? "Estas en cola." : "No estas en cola para este espectaculo.");
        return estado;
    }

    public synchronized void leave(Long espectaculoId, String sesionId) {
        ArrayDeque<ColaEntry> cola = colas.get(espectaculoId);
        if (cola != null) {
            cola.removeIf(entry -> entry.sesionId.equals(sesionId));
        }
        TurnoActivo turno = turnos.get(espectaculoId);
        if (turno != null && turno.sesionId.equals(sesionId)) {
            turnos.remove(espectaculoId);
            promoteIfPossible(espectaculoId);
        }
    }

    public synchronized void assertAccess(Long espectaculoId, String sesionId, String accessToken) {
        Espectaculo espectaculo = requireEspectaculo(espectaculoId);
        if (!requiresQueue(espectaculo)) {
            return;
        }
        if (!isOpen(espectaculo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La taquilla virtual todavia no esta abierta");
        }
        cleanupExpiredTurn(espectaculoId);
        TurnoActivo turno = turnos.get(espectaculoId);
        if (turno == null || !turno.sesionId.equals(sesionId) || !turno.accessToken.equals(accessToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Debes esperar tu turno en la cola virtual");
        }
    }

    @Transactional(readOnly = true)
    public void assertAccessForEntradas(List<Long> entradaIds, String sesionId, String accessToken) {
        if (entradaIds == null || entradaIds.isEmpty()) {
            return;
        }

        Set<Long> espectaculoIds = new LinkedHashSet<>();

        for (Long entradaId : entradaIds) {
            Entrada entrada = entradaDao.findById(entradaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

            if (entrada.getEspectaculo() == null || entrada.getEspectaculo().getId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrada sin espectaculo asociado");
            }

            espectaculoIds.add(entrada.getEspectaculo().getId());
        }

        for (Long espectaculoId : espectaculoIds) {
            assertAccess(espectaculoId, sesionId, accessToken);
        }
    }

    @Scheduled(fixedDelay = 10000)
    public synchronized void cleanupAll() {
        for (Long espectaculoId : List.copyOf(turnos.keySet())) {
            cleanupExpiredTurn(espectaculoId);
            promoteIfPossible(espectaculoId);
        }
    }

    private void promoteIfPossible(Long espectaculoId) {
        if (turnos.containsKey(espectaculoId)) {
            return;
        }
        ArrayDeque<ColaEntry> cola = colas.get(espectaculoId);
        if (cola == null || cola.isEmpty()) {
            return;
        }
        ColaEntry next = cola.removeFirst();
        turnos.put(espectaculoId, new TurnoActivo(next.sesionId));
    }

    private void cleanupExpiredTurn(Long espectaculoId) {
        TurnoActivo turno = turnos.get(espectaculoId);
        if (turno != null && turno.expiresAt < System.currentTimeMillis()) {
            turnos.remove(espectaculoId);
        }
    }

    private int positionOf(ArrayDeque<ColaEntry> cola, String sesionId) {
        if (cola == null) {
            return 0;
        }
        int index = 1;
        for (Iterator<ColaEntry> it = cola.iterator(); it.hasNext(); index++) {
            if (it.next().sesionId.equals(sesionId)) {
                return index;
            }
        }
        return 0;
    }

    private DtoColaEstado activeTurn(Espectaculo espectaculo, TurnoActivo turno) {
        DtoColaEstado estado = base(espectaculo);
        estado.setEnCola(false);
        estado.setTurnoActivo(true);
        estado.setAccessToken(turno.accessToken);
        estado.setSegundosTurnoRestantes(Math.max(0, (turno.expiresAt - System.currentTimeMillis()) / 1000));
        estado.setMessage("Es tu turno. Puedes seleccionar entradas.");
        return estado;
    }

    private DtoColaEstado freeAccess(Espectaculo espectaculo) {
        DtoColaEstado estado = base(espectaculo);
        estado.setTaquillaAbierta(true);
        estado.setTurnoActivo(true);
        estado.setMessage("Este espectaculo no requiere cola virtual.");
        return estado;
    }

    private DtoColaEstado soldOut(Espectaculo espectaculo) {
        DtoColaEstado estado = base(espectaculo);
        estado.setTaquillaAbierta(true);
        estado.setEnCola(false);
        estado.setTurnoActivo(false);
        estado.setEntradasAgotadas(true);
        estado.setMessage("Las entradas para este espectaculo se han agotado.");
        return estado;
    }

    private DtoColaEstado base(Espectaculo espectaculo) {
        DtoColaEstado estado = new DtoColaEstado();
        estado.setRequiereCola(requiresQueue(espectaculo));
        estado.setTaquillaAbierta(isOpen(espectaculo));
        estado.setAperturaTaquilla(espectaculo.getAperturaTaquilla());
        return estado;
    }

    private boolean requiresQueue(Espectaculo espectaculo) {
        return espectaculo.isAltaDemanda();
    }

    private boolean isOpen(Espectaculo espectaculo) {
        return espectaculo.getAperturaTaquilla() == null || !LocalDateTime.now().isBefore(espectaculo.getAperturaTaquilla());
    }

    private boolean isSoldOut(Long espectaculoId) {
        return entradaDao.countByEspectaculoIdAndEstado(espectaculoId, Estado.DISPONIBLE) == 0;
    }

    private Espectaculo requireEspectaculo(Long espectaculoId) {
        return espectaculoDao.findById(espectaculoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectaculo no encontrado"));
    }

    private static class ColaEntry {
        private final String sesionId;
        private ColaEntry(String sesionId) { this.sesionId = sesionId; }
    }

    private static class TurnoActivo {
        private final String sesionId;
        private final String accessToken = UUID.randomUUID().toString();
        private final long expiresAt = System.currentTimeMillis() + TURNO_TTL_MILLIS;
        private TurnoActivo(String sesionId) { this.sesionId = sesionId; }
    }
}
