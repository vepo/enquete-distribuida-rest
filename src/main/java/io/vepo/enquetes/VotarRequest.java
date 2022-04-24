package io.vepo.enquetes;

import java.time.LocalDate;
import java.util.List;

public record VotarRequest(long votanteId, List<LocalDate> opcoes) {
}
