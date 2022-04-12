package io.vepo.enquetes;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@ApplicationScoped
public class Database {

    private List<Usuario> usuarios;

    @PostConstruct
    void setup() {
        usuarios = new ArrayList<>();
    }

    public List<Usuario> usuarios() {
        return usuarios;
    }

    public Usuario criarUsuario(String nome) {
        if (this.usuarios.stream().anyMatch(usuario -> usuario.nome().equals(nome))) {
            throw new WebApplicationException(Response.Status.CONFLICT);
        }
        var usuario = new Usuario(this.usuarios.stream().mapToLong(Usuario::id).max().orElse(0) + 1l, nome);
        usuarios.add(usuario);
        return usuario;
    }

}
