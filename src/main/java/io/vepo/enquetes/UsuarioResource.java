package io.vepo.enquetes;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/usuario")
public class UsuarioResource {
    @Inject
    Database database;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Usuario> listarUsuarios() {
        return database.usuarios();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Usuario criar(CriarUsuarioRequest request) {
        if (database.usuarios().stream().anyMatch(usuario -> usuario.nome().equals(request.nome()))) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        return database.criarUsuario(id -> new Usuario(id, request.nome()));
    }
}
