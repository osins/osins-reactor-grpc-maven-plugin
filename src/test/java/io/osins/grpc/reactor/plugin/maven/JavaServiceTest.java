package io.osins.grpc.reactor.plugin.maven;

import com.google.inject.Guice;
import io.osins.grpc.reactor.plugin.maven.module.ReactiveCodeModule;
import io.osins.grpc.reactor.plugin.maven.service.JavaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import spoon.reflect.declaration.CtClass;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class JavaServiceTest {

    private MavenProject project;

    @BeforeEach
    void init() throws ArtifactResolutionException {
        project = mock(MavenProject.class);

        var root = new File(System.getProperty("user.dir"));
        when(project.getBasedir()).thenReturn(root.getAbsoluteFile());

        var build = new Build();
        build.setDirectory(root.getAbsolutePath()+"/target");

        when(project.getBuild()).thenReturn(build);
    }

    @Test
    void testLoadJavaCodes() throws Exception {
        var source = project.getBasedir()+"/src/test/resources/protobuf/grpc-java";
        var module = Guice.createInjector(new ReactiveCodeModule(project, source, "test-grpc","com.example", "exampleChannel", "/src/test/resources/protobuf/client", ""));
        var javaService = module.getInstance(JavaService.class);

        var launcher = javaService.loadJavaCodes(source);
        var classList = launcher.getModel().getElements(e->e instanceof CtClass)
                .stream().filter(c->{
                    var ctClass = (CtClass<?>) c;
                    log.debug("Class name: {}", ctClass.getSimpleName());      // 简单类名，例如 SysPermissionServiceStub
                    log.debug("Qualified name: {}", ctClass.getQualifiedName()); // 全限定名，例如 com.example.grpc.SysPermissionServiceStub

                    return ctClass.getSimpleName().endsWith("Stub");
                }).map(c -> (CtClass<?>) c).toList();

        assertNotNull(launcher.getModel());
        assertFalse(classList.isEmpty());

        classList.forEach(clazz->{
            log.debug("Analyzing：{}", clazz.getSimpleName());
        });
    }
}