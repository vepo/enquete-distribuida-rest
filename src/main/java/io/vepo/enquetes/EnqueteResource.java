package io.vepo.enquetes;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Arrays;

import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("enquetes")
@ApplicationScoped
public class EnqueteResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Enquete> listarEnquetes() {
        return Arrays.asList(new Enquete("Enquete 1"));
    }
}