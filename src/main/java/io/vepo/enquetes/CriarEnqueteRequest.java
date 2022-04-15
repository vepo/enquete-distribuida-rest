package io.vepo.enquetes;

import java.time.LocalDate;
import java.util.List;

public record CriarEnqueteRequest(String titulo, String criador, String local, List<LocalDate> opcoes) {
    
}
