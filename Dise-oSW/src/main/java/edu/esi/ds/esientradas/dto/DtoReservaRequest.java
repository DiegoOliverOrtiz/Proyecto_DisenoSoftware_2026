package edu.esi.ds.esientradas.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class DtoReservaRequest {
    @NotEmpty
    @Size(max = 8)
    private List<@Positive Long> entradaIds = new ArrayList<>();

    public List<Long> getEntradaIds() {
        return entradaIds;
    }

    public void setEntradaIds(List<@Positive Long> entradaIds) {
        this.entradaIds = entradaIds;
    }
}
