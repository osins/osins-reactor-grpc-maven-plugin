package io.osins.grpc.reactor.plugin.maven;

import com.google.inject.Guice;
import com.google.protobuf.DescriptorProtos;
import io.osins.grpc.reactor.plugin.maven.module.ProtoModule;
import io.osins.grpc.reactor.plugin.maven.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ProtoGenTest {

    private MavenProject project;
    private org.eclipse.aether.RepositorySystem system;
    private MavenSession session;
    private RepositorySystemSession reposSession;
    private RepositorySystem repositorySystem;
    private String outDesc;
    private String outProtobuf;
    private String source;

    @BeforeEach
    void init() throws ArtifactResolutionException {
        var protocGenExePath = "/home/richard/.m2/repository/io/grpc/protoc-gen-grpc-java/1.74.0/protoc-gen-grpc-java-1.74.0-linux-x86_64.exe";

        system = mock(RepositorySystem.class);
        session = mock(MavenSession.class);
        reposSession = mock(RepositorySystemSession.class);
        repositorySystem = mock(RepositorySystem.class);
        project = mock(MavenProject.class);

        var path = Path.of(protocGenExePath);

        var result = mock(ArtifactResult.class);
        var artifact = mock(Artifact.class);

        when(artifact.getFile()).thenReturn(path.toFile());
        when(result.getArtifact()).thenReturn(artifact);

        // Mock RepositorySystem resolve 方法
        when(repositorySystem.resolveArtifact(argThat(session->{
            log.info("Stubbed resolveArtifact called for session: {}", session);
            return true;
        }), argThat(req -> {
            // 打印看看实际传进来的 request
            log.info("Stubbed resolveArtifact called for artifact: {}", req.getArtifact());
            return true; // 全部匹配
        })))
                .thenAnswer(invocation -> {
                    log.info("Resolving artifact: {}", invocation);
                    var req = invocation.getArgument(1);
                    return result;
                });

        // 模拟 MavenProject
        var root = new File(System.getProperty("user.dir"));
        when(project.getBasedir()).thenReturn(root.getAbsoluteFile());

        var build = new Build();
        build.setDirectory(root.getAbsolutePath() + "/target");
        when(project.getBuild()).thenReturn(build);

        outProtobuf = "/protobuf";
        outDesc = "/desc";
        source = "/src/test/resources/grpc/proto";
    }

    @Test
    public void testGetFileDescriptorSet() throws Exception {

        // 创建 Guice Injector
        var threads = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        var injector = Guice.createInjector(new ProtoModule(threads, project, session, reposSession, repositorySystem));

        // 注入 ProtoService
        var protoService = injector.getInstance(ProtoService.class);

        // 调用 ProtoService 获取 FileDescriptorSet
        var result = protoService.getFileDescriptorSet(source, outDesc, outProtobuf);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        // 遍历并打印解析结果
        for (DescriptorProtos.FileDescriptorSet descSet : result) {
            for (DescriptorProtos.FileDescriptorProto f : descSet.getFileList()) {
                log.info("Parsed proto file: {}({}) with {} services", f.getName(), f.getPackage(), f.getServiceCount());

                f.getMessageTypeList().forEach(m -> log.info("Message: {}", m.getName()));
                f.getServiceList().forEach(s -> {
                    log.info("Service: {}", s.getName());
                    s.getMethodList().forEach(m -> log.info("Method: {}", m.getName()));
                });
            }
        }
    }
}
