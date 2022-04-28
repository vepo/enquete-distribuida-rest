package io.vepo.enquetes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class Database {
    private static final Logger logger = LoggerFactory.getLogger(Database.class);
    private List<Usuario> usuarios;
    private List<Enquete> enquetes;
    private Map<Long, Map<Long, List<LocalDate>>> votos;

    @PostConstruct
    void setup() {
        usuarios = Collections.synchronizedList(new ArrayList<>());
        enquetes = Collections.synchronizedList(new ArrayList<>());
        votos = Collections.synchronizedMap(new HashMap<>());
    }

    @Scheduled(every = "1m")
    public void finalizarEnquete() {
        logger.info("Finalizando enquetes...");
        enquetes.stream()
                .filter(e -> !e.isFinalizada() && e.getDataLimite().isBefore(LocalDate.now()))
                .forEach(e -> {
                    e.setFinalizada(true);
                    e.setUltimaAtualizacao(LocalDateTime.now());
                });
    }

    public List<Usuario> usuarios() {
        return usuarios;
    }

    public Usuario criarUsuario(Function<Long, Usuario> fn) {
        synchronized (this.usuarios) {
            var usuario = fn.apply(this.usuarios.stream().mapToLong(Usuario::id).max().orElse(0) + 1l);
            usuarios.add(usuario);
            return usuario;
        }
    }

    public List<Enquete> enquetes() {
        return enquetes;
    }

    public Enquete criarEnquete(Function<Long, Enquete> fn) {
        synchronized (enquetes) {
            var enquete = fn.apply(enquetes.stream().mapToLong(Enquete::getId).max().orElse(0) + 1);
            enquetes.add(enquete);
            return enquete;
        }
    }

    public Optional<List<LocalDate>> voto(long idEnquete, long idUsuario) {
        return Optional.ofNullable(votos.get(idEnquete)).map(uVotos -> uVotos.get(idUsuario));
    }

    public void registrarVoto(long idEnquete, long votanteId, List<LocalDate> opcoes) {
        var votosNaEnquete = votos.computeIfAbsent(idEnquete, id -> Collections.synchronizedMap(new HashMap<>()));
        var enquete = enquetes.stream()
                .filter(e -> e.getId() == idEnquete)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Enquete não encontrada!"));
        votosNaEnquete.put(votanteId, opcoes);
        if (votosNaEnquete.keySet().containsAll(usuarios.stream().filter(u -> u.id() != enquete.getIdCriador())
                .map(Usuario::id).collect(Collectors.toSet()))) {
            enquete.setFinalizada(true);
            enquete.setUltimaAtualizacao(LocalDateTime.now());
        }
    }

    public List<Resultado> resultados(long idEnquete) {
        return votos.computeIfAbsent(idEnquete, id -> Collections.synchronizedMap(new HashMap<>()))
                    .values()
                    .stream()
                    .flatMap(opt -> opt.stream())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                    .entrySet()
                    .stream()
                    .map(entry -> new Resultado(entry.getKey(), entry.getValue().intValue()))
                    .collect(Collectors.toList());
    }

    public Optional<Usuario> encontrarPorUsernamePassword(String username, String password) {
        return usuarios.stream()
                       .filter(u -> u.nome().equals(username) && "12345".equals(password))
                       .findFirst();
    }

    public List<VotoResponse> votos(long idUsuario) {
        return votos.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().containsKey(idUsuario))
                    .map(entry -> new VotoResponse(entry.getKey(), entry.getValue().get(idUsuario)))
                    .collect(Collectors.toList());
    }

}
