package io.osins.grpc.reactor.plugin.maven;

import com.google.inject.Guice;
import io.osins.grpc.reactor.plugin.maven.module.ProtoModule;
import io.osins.grpc.reactor.plugin.maven.module.ReactiveCodeModule;
import io.osins.grpc.reactor.plugin.maven.service.JavaService;
import io.osins.grpc.reactor.plugin.maven.service.ProtoService;
import io.osins.grpc.reactor.plugin.maven.service.ReactiveCodeService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;

@Slf4j
@Mojo(
        name = "generate-grpc-spring-beans",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
@Setter
@Getter
public class PluginMainMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "example-grpc-0", required = true)
    private String serviceName;

    @Parameter(defaultValue = "authGrpcChannel", required = true)
    private String channelName;

    @Parameter(property = "packageName", defaultValue = "com.example", required = true)
    private String packageName;

    @Parameter(property = "source", defaultValue = "/src/test/resources/grpc/proto")
    private String source;

    @Parameter(property = "outDesc", defaultValue = "/src/test/resources/grpc/desc")
    private String outDesc;

    @Parameter(property = "outProtobuf", defaultValue = "/generated-sources/protobuf")
    private String outProtobuf;

    @Parameter(property = "outClient", defaultValue = "/generated-sources/protobuf/client")
    private String outClient;

    @Parameter(property = "utilPath", defaultValue = "/generated-sources/protobuf/utils")
    private String utilPath;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(readonly = true, property = "localRepository")
    private MavenArtifactRepository localRepository;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private DefaultRepositorySystemSession repoSession;

    @Component
    @SuppressWarnings("deprecation")
    private DefaultRepositorySystem repoSystem;

    @Override
    public void execute() throws MojoExecutionException {
        if (project == null) {
            log.warn("project is null");
            return;
        }

        log.info("artifact repositories: {}", project.getRemoteArtifactRepositories());
        log.info("plugin repositories: {}", project.getRemotePluginRepositories());
        log.info("session[{}]: {}", session.getClass().getTypeName(), session);
        log.info("localRepository[{}]: {}", localRepository.getClass().getTypeName(), localRepository);
        log.info("repoSession[{}]: {}", repoSession.getClass().getTypeName(), repoSession);
        log.info("repoSystem[{}]: {}", repoSystem.getClass().getTypeName(), repoSession);

        var threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        var protoInjector = Guice.createInjector(
                new ProtoModule(threads,
                        project,
                        session, repoSession, repoSystem));
        var reactiveCodeInjector = Guice.createInjector(new ReactiveCodeModule(project, outProtobuf, serviceName, packageName, channelName, outClient));

        log.info("Starting gRPC Spring Bean generation...");
        log.info("utilPath: {}", utilPath);

        try {
            // 解析 Proto 文件
            var protoService = protoInjector.getInstance(ProtoService.class);
            var protos = protoService.getFileDescriptorSet(source, outDesc, outProtobuf);

            if (protos.isEmpty()) {
                log.info("No proto files found in: {}", protos);
                return;
            }

            var javaService = reactiveCodeInjector.getInstance(JavaService.class);
            var reactiveCode = reactiveCodeInjector.getInstance(ReactiveCodeService.class);
            reactiveCode.generateSpringWebfluxConfig(javaService.loadJavaCodes(outProtobuf));

            log.info("gRPC Spring Bean generation completed successfully!");

        } catch (Exception e) {
            log.error("Error generating gRPC Spring Beans", e);
            throw new MojoExecutionException("Failed to generate gRPC Spring Beans", e);
        }
    }
}