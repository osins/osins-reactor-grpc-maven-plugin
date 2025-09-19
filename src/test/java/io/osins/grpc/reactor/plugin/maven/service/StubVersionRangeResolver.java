package io.osins.grpc.reactor.plugin.maven.service;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.VersionRangeResolver;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResult;

import static java.util.Objects.requireNonNull;

public class StubVersionRangeResolver implements VersionRangeResolver {

    @Override
    public VersionRangeResult resolveVersionRange(RepositorySystemSession session, VersionRangeRequest request) {
        requireNonNull(session, "session cannot be null");
        requireNonNull(request, "request cannot be null");
        return new VersionRangeResult(request);
    }
}