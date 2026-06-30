package edu.esi.ds.esientradas.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import edu.esi.ds.esientradas.dao.EntradaDao;
import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.dto.DtoEntradaCompra;
import edu.esi.ds.esientradas.dto.DtoEntradas;
import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Precisa;

@Service
public class BusquedaService {

    @Autowired
    private EscenarioDao dao;

    @Autowired
    private EspectaculoDao espectaculoDao;

    @Autowired
    private EntradaDao entradaDao;

    public List<Escenario> getEscenarios() {
        return this.dao.findAll();
    }

    public List<Espectaculo> getEspectaculosPorArtista(String artista) {
        return this.espectaculoDao.findByArtistaContainingIgnoreCase(artista == null ? "" : artista.trim());
    }
    public List<Espectaculo> getEspectaculos(Long idEscenario) {
        return this.espectaculoDao.findByEscenarioId(idEscenario);
    }

    public List<Entrada> getEntradas(Long espectaculoId) {
        return this.entradaDao.findByEspectaculoId(espectaculoId);
    }

    public List<DtoEntradaCompra> getEntradasDisponibles(Long espectaculoId) {
        return this.entradaDao.findByEspectaculoIdAndEstadoOrderByIdAsc(espectaculoId, Estado.DISPONIBLE).stream()
                .map(this::crearDtoCompra)
                .toList();
    }

    public Integer getNumeroDeEntradas(Long espectaculoId) {
        return this.entradaDao.countByEspectaculoId(espectaculoId);
    }

    public Integer getEntradasLibres(Long espectaculoId) {
        return this.entradaDao.countByEspectaculoIdAndEstado(espectaculoId, edu.esi.ds.esientradas.model.Estado.DISPONIBLE);
    }

    public DtoEntradas getNumeroDeEntradasComoDto(Long espectaculoId) {
        Object o = this.entradaDao.getNumeroDeEntradasComoDto(espectaculoId);
        // Cast the result to DtoEntradas (assuming the query returns an array of objects)
        DtoEntradas dto = new DtoEntradas();
        Object[] row = (Object[]) o;
        dto.setTotal(((Number) row[0]).intValue());
        dto.setLibres(((Number) row[1]).intValue());    
        dto.setReservadas(((Number) row[2]).intValue());
        dto.setVendidas(((Number) row[3]).intValue());
        return dto;
    }

    public Espectaculo getEspectaculoById(Long id) {
        return this.espectaculoDao.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espectaculo no encontrado"));
    }

    private DtoEntradaCompra crearDtoCompra(Entrada entrada) {
        return new DtoEntradaCompra(entrada.getId(), this.descripcionEntrada(entrada), entrada.getPrecio());
    }

    private String descripcionEntrada(Entrada entrada) {
        if (entrada instanceof Precisa precisa) {
            return "Asiento planta " + precisa.getPlanta() + ", fila " + precisa.getFila() + ", columna " + precisa.getColumna();
        }
        if (entrada instanceof DeZona zona) {
            return "Zona " + zona.getZona();
        }
        return "Entrada " + entrada.getId();
    }

}
