package io.vepo.enquetes;

import java.time.LocalDate;
import java.util.List;

public record Enquete(int id, String titulo, String criador, String local, List<LocalDate> opcoes) {

}
