package io.vepo.infra;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vepo.enquetes.Database;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class BasicAuthAuthenticationMechanism implements HttpAuthenticationMechanism {
    private static final String PREFIX = "BASIC ";
    private static final Logger logger = LoggerFactory.getLogger(BasicAuthAuthenticationMechanism.class);

    @Inject
    Database database;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        var credential = context.request().headers().get(HttpHeaderNames.AUTHORIZATION);
        if (Objects.isNull(credential) || credential.isEmpty() || !credential.toUpperCase().startsWith(PREFIX)) {
            return Uni.createFrom().optional(Optional.empty());
        }

        var base64Credentials = credential.substring(PREFIX.length()).trim();
        try {
            var pair = new String(Base64.getDecoder().decode(base64Credentials), "windows-1252").split(":");
            if (pair.length != 2) {
                return Uni.createFrom().failure(new AuthenticationFailedException());
            }

            return Uni.createFrom()
                    .optional(database.encontrarPorUsernamePassword(pair[0], pair[1])
                                        .map(usuario -> QuarkusSecurityIdentity.builder()
                                                                            .setPrincipal(new QuarkusPrincipal(usuario.nome()))
                                                                            .addRoles(Set.of("ADMIN"))
                                                                            .build()));
        } catch (UnsupportedEncodingException uee) {
            logger.error("Error ao ler charset!", uee);
            return Uni.createFrom().failure(new AuthenticationFailedException());
        }
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        ChallengeData challengeData = new ChallengeData(HttpResponseStatus.FORBIDDEN.code(), null, null);
        return Uni.createFrom().item(challengeData);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of(CredenciaisUsuario.class);
    }

    @Override
    public HttpCredentialTransport getCredentialTransport() {
        return new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "MyBasic");
    }

}