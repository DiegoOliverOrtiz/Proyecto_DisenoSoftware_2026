package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.esi.ds.esientradas.model.Espectaculo;


public interface EspectaculoDao extends JpaRepository<Espectaculo, Long> { //Entidad que se gestiona y su clave (En tipo variable)
     @EntityGraph(attributePaths = "escenario")
     List<Espectaculo> findByArtistaContainingIgnoreCase(String artista);
     
     @EntityGraph(attributePaths = "escenario")
     List<Espectaculo> findByEscenarioId(Long idEscenario);

     @Override
     @EntityGraph(attributePaths = "escenario")
     java.util.Optional<Espectaculo> findById(Long id);
}
