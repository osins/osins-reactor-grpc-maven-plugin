package io.osins.grpc.reactor.plugin.maven.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.osins.grpc.reactor.plugin.maven.service.JavaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import spoon.Launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class JavaServiceImpl implements JavaService {
    private final MavenProject project;
    @Inject
    @Named("outClient")
    private String outClient;

    @Override
    public Launcher loadJavaCodes(String output) throws IOException {
        var path = Paths.get(project.getBuild().getDirectory(), "/generated-sources", output);
        if (!path.toFile().exists()) {
            throw new IOException("load java codes, error, source path not exists: " + path.toAbsolutePath());
        }

        var grpcJavaPath = path.resolve("grpc-java").toAbsolutePath().toString();
        var javaPath = path.resolve("java").toAbsolutePath().toString();

        // 设置可写
        if (!path.toFile().setWritable(true, false)) { // false 表示对所有用户可写
            log.warn("Failed to set writable permission for: {}", grpcJavaPath);
        }

        log.info("load java codes, path: {}", grpcJavaPath);

        log.info("out client path: {}", outClient);

        var outPath = Paths.get(outClient);
        if(!outPath.toFile().exists())
            Files.createDirectories(outPath);

        var basePath = "/home/richard/codes/matrix/matrix-shared/matrix-shared-grpc/matrix-shared-grpc-base/src/main/java/club/hm/matrix/shared/grpc/base/utils";
        var packages = Stream.concat(project.getArtifacts().stream()
                                .filter(artifact -> artifact.getFile().exists())
                                .map(artifact -> artifact.getFile().getAbsolutePath()),
                        Stream.of(grpcJavaPath, javaPath, basePath, outClient))
                .toList();

        var launcher = new Launcher();
        var env = launcher.getEnvironment();
        env.setNoClasspath(false); // 避免类路径冲突
        env.setComplianceLevel(Integer.parseInt(project.getProperties().getProperty("maven.compiler.source")));
        env.setAutoImports(true);  // 自动导入
        env.setCommentEnabled(true);
        env.setSourceClasspath(packages.toArray(String[]::new));

        log.info("JavaServiceImpl loadJavaCodes, java version: {}", env.getComplianceLevel());
        log.info("JavaServiceImpl loadJavaCodes, source classpath: {}", Arrays.toString(env.getSourceClasspath()));

        launcher.addInputResource(grpcJavaPath);
        launcher.buildModel();

        launcher.getFactory().getEnvironment().setSourceOutputDirectory(Paths.get(grpcJavaPath).toFile());

        launcher.setSourceOutputDirectory(outClient);

        return launcher;
    }
}
