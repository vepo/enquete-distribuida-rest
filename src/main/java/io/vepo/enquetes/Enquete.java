package io.vepo.enquetes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class Enquete {
    private final long id;
    private final String titulo;
    private final long idCriador;
    private final String local;
    private final LocalDate dataLimite;
    private final List<LocalDate> opcoes;
    private LocalDateTime ultimaAtualizacao;
    private boolean finalizada;

    public Enquete(long id, 
                   String titulo, 
                   long idCriador, 
                   String local, 
                   LocalDate dataLimite,
                   List<LocalDate> opcoes) {
        this.id = id;
        this.titulo = titulo;
        this.idCriador = idCriador;
        this.local = local;
        this.dataLimite = dataLimite;
        this.opcoes = opcoes;
        this.ultimaAtualizacao = LocalDateTime.now();
        this.finalizada = false;
    }

    public long getIdCriador() {
        return idCriador;
    }

    public LocalDate getDataLimite() {
        return dataLimite;
    }

    public long getId() {
        return id;
    }

    public String getLocal() {
        return local;
    }

    public List<LocalDate> getOpcoes() {
        return opcoes;
    }

    public String getTitulo() {
        return titulo;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public boolean isFinalizada() {
        return finalizada;
    }

    public void setFinalizada(boolean finalizada) {
        this.finalizada = finalizada;
    }

    public void setUltimaAtualizacao(LocalDateTime ultimaAtualizacao) {
        this.ultimaAtualizacao = ultimaAtualizacao;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, idCriador, local, dataLimite);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        Enquete other = (Enquete) obj;
        return Objects.equals(id, other.id) && 
               Objects.equals(titulo, other.titulo) &&
               idCriador == other.idCriador &&
               Objects.equals(local, other.local) &&
               Objects.equals(dataLimite, other.dataLimite) &&
               Objects.equals(finalizada, other.finalizada) &&
               Objects.equals(ultimaAtualizacao, other.ultimaAtualizacao);
    }

    @Override
    public String toString() {
        return String.format("Enquete [id=%d, titulo=%s, idCriador=%d, dataLimite=%s, local=%s, opcoes=%s, finalizada=%s, ultimaAtualizacao=%s]", 
                            id, titulo, idCriador, dataLimite, local, opcoes, finalizada, ultimaAtualizacao);
    }
}
