package io.vepo.enquetes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("enquetes")
@ApplicationScoped
public class EnqueteResource {

    private static final Logger logger = LoggerFactory.getLogger(EnqueteResource.class);

    @Inject
    Database database;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Enquete> listarEnquetes() {
        logger.info("Listando enquetes...");
        return database.enquetes();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Enquete criarEnquete(CriarEnqueteRequest request) {
        if (database.enquetes().stream().anyMatch(enquete-> enquete.getTitulo().equals(request.titulo()))) {
            throw new WebApplicationException(String.format("Já existe uma enquete com mesmo título! tituloe=%s", request.titulo()), Status.CONFLICT);
        }
        logger.info("Criando enquetes... request={}", request);
        return this.database.criarEnquete(id -> new Enquete(id, request.titulo(), request.idCriador(),
                                                            request.local(), request.dataLimite(), request.opcoes()));
    }

    @PUT
    @Path("{idEnquete}/votar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response votar(@PathParam("idEnquete") long idEnquete, VotarRequest request) {
        if (database.enquetes().stream().noneMatch(enquete -> enquete.getId() == idEnquete)) {
            return Response.status(Status.NOT_FOUND).build();
        } else if (database.enquetes().stream().anyMatch(enquete -> enquete.getId() == idEnquete) && database.voto(idEnquete, request.votanteId()).isPresent()) {
            return Response.status(Status.CONFLICT).build();
        }
        database.registrarVoto(idEnquete, request.votanteId(), request.opcoes());
        return Response.noContent().build();
    }

    @GET
    @Path("{idEnquete}/resultados")
    public List<Resultado> verResultados(@PathParam("idEnquete") long idEnquete) {
        return database.resultados(idEnquete);
    }

    @GET
    @Path("stream/{idUsuario}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void enquetesStream(@PathParam("idUsuario") Long idUsuario, @Context SseEventSink sseEventSink, @Context Sse sse) {
        logger.info("Iniciando stream de enquetes...");
        var lastEventId = new AtomicLong(database.enquetes().stream().mapToLong(Enquete::getId).max().orElse(0));
        var ultimaAtualizacao = new AtomicReference<>(database.enquetes().stream()
                                                                         .map(Enquete::getUltimaAtualizacao)
                                                                         .sorted(Comparator.reverseOrder())
                                                                         .findFirst()
                                                                         .orElseGet(LocalDateTime::now));
        var eventBuilder = sse.newEventBuilder();
        while (!sseEventSink.isClosed()) {
            // Se nenhuma enquete encontrada, espera por novas enquetes
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                sseEventSink.close();
                Thread.currentThread().interrupt();
            }
            // Para todas enquetes novas, envia para cliente
            database.enquetes()
                    .stream()
                    .filter(e -> e.getId() > lastEventId.get())
                    .forEachOrdered(enquete -> {
                        logger.info("Nova enquete! enquete={}", enquete);
                        var sseEvent = eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .data(Evento.class, new Evento<Enquete>("NOVA_ENQUETE", enquete))
                                .build();
                        sseEventSink.send(sseEvent);
                        lastEventId.set(enquete.getId());
                        logger.info("Enquete enviada! usuarioId={} enquete={}", idUsuario, enquete);
                    });
            database.enquetes()
                    .stream()
                    .filter(e -> e.isFinalizada() & e.getUltimaAtualizacao().isAfter(ultimaAtualizacao.get()))
                    .sorted(Comparator.comparing(Enquete::getUltimaAtualizacao))
                    .forEachOrdered(enquete -> {
                        if (enquete.getIdCriador() == idUsuario || database.voto(enquete.getId(), idUsuario).isPresent()) {
                            logger.info("Enquete finalizada! enquete={}", enquete);
                            var sseEvent = eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE)
                                    .data(Evento.class, new Evento<Enquete>("ENQUETE_FINALIZADA", enquete))
                                    .build();
                            sseEventSink.send(sseEvent);
                            ultimaAtualizacao.set(enquete.getUltimaAtualizacao());
                            logger.info("Enquete finalizada enviada! usuarioId={} enquete={}", idUsuario, enquete);
                        }
                    });
        }
        sseEventSink.close();
        logger.info("Stream finalizado!");
    }
}