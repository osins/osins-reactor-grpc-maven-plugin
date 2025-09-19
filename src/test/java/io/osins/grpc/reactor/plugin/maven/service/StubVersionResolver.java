package io.osins.grpc.reactor.plugin.maven.service;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;

import static java.util.Objects.requireNonNull;

public class StubVersionResolver implements VersionResolver {

    @Override
    public VersionResult resolveVersion(RepositorySystemSession session, VersionRequest request)
            throws VersionResolutionException {
        requireNonNull(session, "session cannot be null");
        requireNonNull(request, "request cannot be null");
        return new VersionResult(request);
    }
}