package edu.esi.ds.esientradas.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.esi.ds.esientradas.model.Token;


public interface TokenDao extends JpaRepository<Token, String> { //Entidad que se gestiona y su clave (En tipo variable)
    @Query("SELECT t FROM Token t JOIN FETCH t.entrada WHERE t.sesionId = :sesionId")
    List<Token> findBySesionId(@Param("sesionId") String sesionId);

    @Query("SELECT t FROM Token t JOIN FETCH t.entrada WHERE t.entrada.id = :entradaId")
    List<Token> findAllByEntradaId(@Param("entradaId") Long entradaId);

    @Query("SELECT t FROM Token t JOIN FETCH t.entrada WHERE t.horaActiva < :horaLimite")
    List<Token> findExpiredWithEntrada(@Param("horaLimite") long horaLimite);

    void deleteBySesionId(String sesionId);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.entrada.id = :entradaId")
    void deleteByEntradaId(@Param("entradaId") Long entradaId);

    @Query("SELECT t FROM Token t JOIN FETCH t.entrada WHERE t.horaActiva <= :threshold")
    List<Token> findByHoraActivaLessThanEqual(@Param("threshold") long threshold);
}
