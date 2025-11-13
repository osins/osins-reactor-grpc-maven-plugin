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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class JavaServiceImpl implements JavaService {
    private final MavenProject project;
    @Inject
    @Named("outClient")
    private String outClient;

    @Inject
    @Named("utilPath")
    private String utilPath;

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

        // 将项目依赖、生成的代码路径和protobuf相关依赖都添加到类路径
        var protobufArtifacts = project.getArtifacts().stream()
                .filter(artifact -> 
                    artifact.getGroupId().contains("protobuf") || 
                    artifact.getGroupId().contains("grpc") ||
                    artifact.getArtifactId().contains("protobuf") ||
                    artifact.getArtifactId().contains("grpc")
                )
                .map(artifact -> artifact.getFile().getAbsolutePath());

        var allArtifacts = project.getArtifacts().stream()
                .filter(artifact -> artifact.getFile().exists())
                .map(artifact -> artifact.getFile().getAbsolutePath());

        var packages = Stream.concat(
                Stream.concat(allArtifacts, protobufArtifacts),
                Stream.of(grpcJavaPath, javaPath, utilPath, outClient))
                .distinct() // 去重
                .toList();

        var launcher = new Launcher();
        var env = launcher.getEnvironment();
        env.setNoClasspath(true); // 避免类路径冲突
        // 设置源码兼容级别，如果项目属性中没有则默认为21
        String sourceVersion = project.getProperties().getProperty("maven.compiler.source");
        int complianceLevel = 21; // 默认为21
        if (sourceVersion != null) {
            try {
                complianceLevel = Integer.parseInt(sourceVersion);
            } catch (NumberFormatException e) {
                log.warn("Could not parse source version: {}, using default 21", sourceVersion);
            }
        }
        env.setComplianceLevel(complianceLevel);
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
