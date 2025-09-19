package io.osins.grpc.reactor.plugin.maven.service;


import java.util.Collection;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.spi.synccontext.SyncContextFactory;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public class StubSyncContextFactory implements SyncContextFactory {

    @Override
    public SyncContext newInstance(RepositorySystemSession session, boolean shared) {
        requireNonNull(session, "session cannot be null");
        return new SyncContext() {
            @Override
            public void close() {}

            @Override
            public void acquire(Collection<? extends Artifact> artifacts, Collection<? extends Metadata> metadatas) {}
        };
    }
}