package io.vepo.enquetes;

import java.time.LocalDate;
import java.util.List;

public record CriarEnqueteRequest(String titulo, Long idCriador, String local, LocalDate dataLimite, List<LocalDate> opcoes) {
    
}
