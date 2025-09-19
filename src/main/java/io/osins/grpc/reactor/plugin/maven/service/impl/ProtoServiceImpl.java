package io.osins.grpc.reactor.plugin.maven.service.impl;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.DescriptorProtos;
import io.osins.grpc.reactor.plugin.maven.service.ProtocCompiler;
import io.osins.grpc.reactor.plugin.maven.service.ProtoService;
import io.osins.grpc.reactor.plugin.maven.service.ProtocCompilerService;
import io.osins.grpc.reactor.plugin.maven.service.ProtocPluginDownloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.PreDestroy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Stream;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProtoServiceImpl implements ProtoService {
    private final ProtocCompiler protocCompiler;
    private final ExecutorService executor;
    private final MavenProject project;
    private final ProtocPluginDownloader protocPluginDownloader;

    @Override
    public List<DescriptorProtos.FileDescriptorSet> getFileDescriptorSet(String source, String outDesc, String outProtobuf) {
        var protoPath = project.getBasedir() + source;

        log.info("Loading proto files from: {}", protoPath);

        if (!Files.exists(Paths.get(protoPath)))
            throw new RuntimeException("proto file not found: " + protoPath);

        try (Stream<Path> paths = Files.walk(Paths.get(protoPath))) {
            var futures = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".proto"))
                    .map(path -> CompletableFuture.supplyAsync(() -> compileAndLoadDescriptor(path, outDesc, outProtobuf), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            log.error("Error loading proto files: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    public DescriptorProtos.FileDescriptorSet compileAndLoadDescriptor(Path protoPath, String outDesc, String outProtobuf) {
        log.info("Loading proto file: {}", protoPath);

        try {
            var baseName = FilenameUtils.getBaseName(protoPath.getFileName().toString());
            var targetPath = Path.of(project.getBuild().getDirectory() + "/generated-sources");
            var descPath = Path.of(targetPath.toAbsolutePath() + outDesc);
            var generatedPath = Path.of(targetPath.toAbsolutePath() + outProtobuf);
            var javaPath = generatedPath.resolve("java");
            var grpcJavaPath = generatedPath.resolve("grpc-java");

            Files.createDirectories(descPath);
            Files.createDirectories(javaPath);
            Files.createDirectories(grpcJavaPath);

            var protocGenGrpcJavaPath = protocPluginDownloader.resolveProtocGenGrpcJava();

            var descFile = descPath.resolve(baseName + ".desc");
            var request = ProtocCompilerService.ProtocCompileRequest.builder()
                    .protocExecutable("protoc")
                    .protocGenGrpcJavaPath(protocGenGrpcJavaPath)
                    .protoDir(protoPath.getParent())
                    .protoFileNames(List.of(protoPath.getFileName().toString()))
                    .javaOutDir(javaPath)
                    .grpcJavaOutDir(grpcJavaPath)
                    .descriptorSetOut(descFile)
                    .includeImports(true)
                    .timeout(Duration.ofSeconds(60))
                    .build();

            protocCompiler.compile(request);

            var descriptorBytes = Files.readAllBytes(descFile);
            return DescriptorProtos.FileDescriptorSet.parseFrom(descriptorBytes);

        } catch (Exception e) {
            log.error("Error compiling proto file: {}", protoPath, e);
            return null;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
