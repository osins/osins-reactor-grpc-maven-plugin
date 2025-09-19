package io.osins.grpc.reactor.plugin.maven.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.osins.grpc.reactor.plugin.maven.service.ProtoService;
import io.osins.grpc.reactor.plugin.maven.service.ProtocCompiler;
import io.osins.grpc.reactor.plugin.maven.service.impl.ProtoServiceImpl;
import io.osins.grpc.reactor.plugin.maven.service.impl.ProtocCompilerImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
@RequiredArgsConstructor
public class ProtoModule extends AbstractModule {
    private final int threadCount;
    private final MavenProject project;
    private final MavenSession session;
    private final RepositorySystemSession repositorySystemSession;
    private final RepositorySystem repositorySystem;

    @Override
    protected void configure() {
        bind(ProtocCompiler.class).to(ProtocCompilerImpl.class);
        bind(ProtoService.class).to(ProtoServiceImpl.class);
        bind(MavenProject.class).toInstance(project);
        bind(MavenSession.class).toInstance(session);
        bind(RepositorySystemSession.class).toInstance(repositorySystemSession);
        bind(RepositorySystem.class).toInstance(repositorySystem);
    }

    @Provides
    @Singleton
    ExecutorService provideExecutorService() {
        return Executors.newFixedThreadPool(threadCount);
    }
}