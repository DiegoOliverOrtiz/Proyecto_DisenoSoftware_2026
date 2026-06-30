package edu.esi.ds.esientradas.config;

import java.time.LocalDateTime;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.esi.ds.esientradas.dao.EscenarioDao;
import edu.esi.ds.esientradas.dao.EspectaculoDao;
import edu.esi.ds.esientradas.model.DeZona;
import edu.esi.ds.esientradas.model.Entrada;
import edu.esi.ds.esientradas.model.Escenario;
import edu.esi.ds.esientradas.model.Espectaculo;
import edu.esi.ds.esientradas.model.Estado;
import edu.esi.ds.esientradas.model.Precisa;
import jakarta.annotation.PostConstruct;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DevDataInitializer {
    private final EscenarioDao escenarioDao;
    private final EspectaculoDao espectaculoDao;

    public DevDataInitializer(EscenarioDao escenarioDao, EspectaculoDao espectaculoDao) {
        this.escenarioDao = escenarioDao;
        this.espectaculoDao = espectaculoDao;
    }

    @PostConstruct
    @Transactional
    public void seedIfEmpty() {
        if (escenarioDao.count() > 0 || espectaculoDao.count() > 0) {
            ensureHighDemandDemoEvents();
            return;
        }

        Escenario estadio = escenario("Estadi Olimpic Lluis Companys", "Recinto de gran formato para conciertos en Barcelona.");
        Escenario teatro = escenario("Teatro Real", "Teatro con butacas numeradas por planta, fila y asiento.");
        Escenario metropolitano = escenario("Estadio Riyadh Air Metropolitano", "Estadio para conciertos multitudinarios en Madrid.");

        escenarioDao.save(estadio);
        escenarioDao.save(teatro);
        escenarioDao.save(metropolitano);

        espectaculoDao.save(espectaculoConZonas("Bad Bunny", LocalDateTime.of(2026, 5, 22, 20, 0), estadio));
        espectaculoDao.save(espectaculoConZonas("Bad Bunny - Paquetes VIP", LocalDateTime.of(2026, 5, 23, 20, 0), estadio));
        espectaculoDao.save(espectaculoConZonas("Radiohead", LocalDateTime.of(2027, 6, 1, 21, 0), metropolitano));

        Espectaculo natos = espectaculoConButacas("Natos y Waor", LocalDateTime.of(2026, 3, 14, 18, 0), teatro);
        natos.setAltaDemanda(true);
        natos.setAperturaTaquilla(LocalDateTime.of(2026, 5, 7, 18, 0));
        espectaculoDao.save(natos);
    }

    private void ensureHighDemandDemoEvents() {
        markHighDemand("Natos y Waor", LocalDateTime.of(2026, 5, 7, 18, 0));
        unmarkHighDemand("Radiohead");
    }

    private void markHighDemand(String artista, LocalDateTime apertura) {
        for (Espectaculo espectaculo : espectaculoDao.findByArtistaContainingIgnoreCase(artista)) {
            espectaculo.setAltaDemanda(true);
            espectaculo.setAperturaTaquilla(apertura);
            espectaculoDao.save(espectaculo);
        }
    }

    private void unmarkHighDemand(String artista) {
        for (Espectaculo espectaculo : espectaculoDao.findByArtistaContainingIgnoreCase(artista)) {
            espectaculo.setAltaDemanda(false);
            espectaculo.setAperturaTaquilla(null);
            espectaculoDao.save(espectaculo);
        }
    }

    private Escenario escenario(String nombre, String descripcion) {
        Escenario escenario = new Escenario();
        escenario.setNombre(nombre);
        escenario.setDescripcion(descripcion);
        return escenario;
    }

    private Espectaculo espectaculoConZonas(String artista, LocalDateTime fecha, Escenario escenario) {
        Espectaculo espectaculo = espectaculo(artista, fecha, escenario);
        for (int zona = 1; zona <= 3; zona++) {
            for (int index = 0; index < 8; index++) {
                DeZona entrada = new DeZona();
                entrada.setZona(zona);
                entrada.setPrecio(4500L + zona * 1500L);
                addEntrada(espectaculo, entrada);
            }
        }
        return espectaculo;
    }

    private Espectaculo espectaculoConButacas(String artista, LocalDateTime fecha, Escenario escenario) {
        Espectaculo espectaculo = espectaculo(artista, fecha, escenario);
        for (int planta = 1; planta <= 2; planta++) {
            for (int fila = 1; fila <= 4; fila++) {
                for (int columna = 1; columna <= 5; columna++) {
                    Precisa entrada = new Precisa();
                    entrada.setPlanta(planta);
                    entrada.setFila(fila);
                    entrada.setColumna(columna);
                    entrada.setPrecio(3500L + planta * 1000L + fila * 250L);
                    addEntrada(espectaculo, entrada);
                }
            }
        }
        return espectaculo;
    }

    private Espectaculo espectaculo(String artista, LocalDateTime fecha, Escenario escenario) {
        Espectaculo espectaculo = new Espectaculo();
        espectaculo.setArtista(artista);
        espectaculo.setFecha(fecha);
        espectaculo.setEscenario(escenario);
        return espectaculo;
    }

    private void addEntrada(Espectaculo espectaculo, Entrada entrada) {
        entrada.setEspectaculo(espectaculo);
        entrada.setEstado(Estado.DISPONIBLE);
        espectaculo.getEntradas().add(entrada);
    }
}
