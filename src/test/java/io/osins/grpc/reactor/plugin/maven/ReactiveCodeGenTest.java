package io.osins.grpc.reactor.plugin.maven;

import com.google.common.reflect.TypeToken;
import com.google.inject.Guice;
import io.osins.grpc.reactor.plugin.maven.module.ReactiveCodeModule;
import io.osins.grpc.reactor.plugin.maven.service.JavaService;
import io.osins.grpc.reactor.plugin.maven.service.ReactiveCodeService;
import io.osins.grpc.reactor.plugin.maven.service.impl.ReactiveCodeServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.ModifierKind;

import java.nio.file.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
class ReactiveCodeGenTest {

    private MavenProject project;

    @BeforeEach
    void init() throws Exception {
        project = mock(MavenProject.class);

        var properties = new Properties();
        var root = new File(System.getProperty("user.dir"));

        when(project.getBasedir()).thenReturn(root.getAbsoluteFile());
        when(project.getProperties()).thenReturn(properties);

        var build = new Build();
        build.setDirectory(root.getAbsolutePath() + "/target");

        when(project.getBuild()).thenReturn(build);
    }

    @Test
    void testGenerateGrpcClientConfig_fromStub() throws Exception {
        var source = "/protobuf/grpc-java";

        var injector = Guice.createInjector(new ReactiveCodeModule(project, source,"test-grpc" ,"com.example", "channelChannel", "/src/test/resources/protobuf/client", ""));
        var javaService = injector.getInstance(JavaService.class);
        var reactiveCode = injector.getInstance(ReactiveCodeService.class);

        var launcher = javaService.loadJavaCodes(source);
        reactiveCode.generateSpringWebfluxConfig(launcher);

        var path = Paths.get(project.getBuild().getDirectory(), "/generated-sources", source, "/com/example/config/GrpcClientConfig.java");
        log.info("generate spring webflux config, path: {}", path.toAbsolutePath());

        assertTrue(path.toFile().exists());
    }

    @Test
    void testGenerateGrpcClientConfig() throws Exception {
        var source = "/home/richard/codes/maven-osins-reactor-grpc-plugin/target/generated-sources/protobuf/grpc-java/com/example/grpc/SysPermissionServiceGrpc.java";
        log.info("generate spring webflux config, source: {}", source);

        var launcher = new Launcher();
        var env = launcher.getEnvironment();
        env.setNoClasspath(true); // 避免类路径冲突
        env.setComplianceLevel(17);
        env.setAutoImports(true);  // 自动导入
        env.setCommentEnabled(true);
        env.setSourceClasspath(List.of(source,
                "/home/richard/.m2/repository/com/google/protobuf/protobuf-java/4.31.1/protobuf-java-4.31.1.jar",
                "/home/richard/.m2/repository/io/grpc/grpc-stub/1.74.0/grpc-stub-1.74.0.jar",
                "/home/richard/.m2/repository/io/grpc/grpc-protobuf/1.74.0/grpc-protobuf-1.74.0.jar",
                "/home/richard/.m2/repository/org/projectlombok/lombok/1.18.38/lombok-1.18.38.jar")
                .toArray(String[]::new));

        launcher.addInputResource(source);
        launcher.buildModel();

        var factory = launcher.getFactory();

        // 1. 创建 GrpcClientConfig 类
        var configClass = factory.Class().create("config.GrpcClientConfig");
        configClass.addModifier(ModifierKind.PUBLIC);

        (new ReactiveCodeServiceImpl("test-grpc", "com.example", "channelChannel", "/src/test/resources/protobuf/client")).buildStubMethod(launcher.getModel().getElements(e -> e instanceof CtClass).stream()
                .map(c -> (CtClass<?>) c).filter(c -> c.getSimpleName().endsWith("Grpc")).findFirst().orElseThrow(), factory);

        launcher.setOutputFilter(configClass.getQualifiedName());
        launcher.setSourceOutputDirectory("/tmp");
        launcher.prettyprint();
    }
}
