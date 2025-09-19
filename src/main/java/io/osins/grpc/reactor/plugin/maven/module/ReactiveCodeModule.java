package io.osins.grpc.reactor.plugin.maven.module;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.osins.grpc.reactor.plugin.maven.service.JavaService;
import io.osins.grpc.reactor.plugin.maven.service.ReactiveCodeService;
import io.osins.grpc.reactor.plugin.maven.service.impl.JavaServiceImpl;
import io.osins.grpc.reactor.plugin.maven.service.impl.ReactiveCodeServiceImpl;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

@Slf4j
@Getter
@RequiredArgsConstructor
public class ReactiveCodeModule extends AbstractModule {
    private final MavenProject project;
    private final String outputDirectory;
    private final String serviceName;
    private final String packageName;
    private final String channelName;
    private final String outClient;

    @Override
    protected void configure() {
        bind(JavaService.class).to(JavaServiceImpl.class);
        bind(ReactiveCodeService.class).to(ReactiveCodeServiceImpl.class);
        bind(MavenProject.class).toInstance(project);

        bind(String.class)
                .annotatedWith(Names.named("outputDirectory"))
                .toInstance(outputDirectory);

        log.info("ReactiveCodeModule configure, output directory: {}", outputDirectory);

        bind(String.class)
                .annotatedWith(Names.named("serviceName"))
                .toInstance(serviceName);

        log.info("ReactiveCodeModule configure, service name: {}", serviceName);

        bind(String.class)
                .annotatedWith(Names.named("packageName"))
                .toInstance(packageName);

        log.info("ReactiveCodeModule configure, package name: {}", packageName);

        bind(String.class)
                .annotatedWith(Names.named("channelName"))
                .toInstance(channelName);

        log.info("ReactiveCodeModule configure, channel name: {}", channelName);

        bind(String.class)
                .annotatedWith(Names.named("outClient"))
                .toInstance(outClient);

        log.info("ReactiveCodeModule configure, out client: {}", outClient);
    }
}