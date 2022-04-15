package io.vepo.enquetes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Multi;

@Path("enquetes")
@ApplicationScoped
public class EnqueteResource {

    private static final Logger logger = LoggerFactory.getLogger(EnqueteResource.class);

    private List<Enquete> enquetes;

    @PostConstruct
    void setup() {
        enquetes = Collections.synchronizedList(new ArrayList<>());
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Enquete> listarEnquetes() {
        logger.info("Listando enquetes...");
        return enquetes;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Enquete criarEnquete(CriarEnqueteRequest request) {
        logger.info("Criando enquetes... request={}", request);
        synchronized (enquetes) {
            var nextId = enquetes.stream().mapToInt(Enquete::id).max().orElse(0) + 1;
            var enquete = new Enquete(nextId, request.titulo(), request.criador(), request.local(), request.opcoes());
            enquetes.add(enquete);
            enquetes.notifyAll();
            return enquete;
        }
    }

    @GET
    @Path("stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void enquetesStream(@Context SseEventSink sseEventSink, @Context Sse sse) {
        logger.info("Iniciando stream de enquetes...");
        AtomicInteger lastEventId = new AtomicInteger(enquetes.stream().mapToInt(Enquete::id).max().orElse(0));
        var eventBuilder = sse.newEventBuilder();
        while (!sseEventSink.isClosed()) {
            // Se nenhuma enquete encontrada, espera por novas enquetes
            if (enquetes.stream().filter(e -> e.id() > lastEventId.get()).count() == 0L) {
                synchronized (enquetes) {
                    try {
                        logger.info("Waiting for new enquetes...");
                        enquetes.wait();
                        logger.info("Awake!");
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            // Para todas enquetes novas, envia para cliente
            enquetes.stream()
                    .filter(e -> e.id() > lastEventId.get())
                    .forEachOrdered(enquete -> {
                        logger.info("Nova enquete! enquete={}", enquete);
                        var sseEvent = eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE)
                                .data(Enquete.class, enquete)
                                .build();
                        sseEventSink.send(sseEvent);
                        lastEventId.set(enquete.id());
                        logger.info("Enquete enviada! id={}", enquete.id());
                    });
        }
        sseEventSink.close();
        logger.info("Stream finalizado!");
    }
}