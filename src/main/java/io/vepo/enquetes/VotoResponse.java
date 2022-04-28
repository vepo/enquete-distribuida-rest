package io.vepo.enquetes;

import java.time.LocalDate;
import java.util.List;

public record VotoResponse(long idEnquete, List<LocalDate> opcoes) {
}
