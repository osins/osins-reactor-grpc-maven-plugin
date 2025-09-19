package io.osins.grpc.reactor.plugin.maven.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class ProtocPluginDownloader {

    private final MavenSession session;
    private final MavenProject project;
    private final RepositorySystem repositorySystem; // 注入标准 Aether RepositorySystem

    private String cachedPath;

    public synchronized String resolveProtocGenGrpcJava() {
        return resolve("io.grpc", "protoc-gen-grpc-java", detectOsClassifier(), "exe", "1.74.0");
    }

    public synchronized String resolve(String groupId, String artifactId, String classifier, String extension, String version) {
        if (cachedPath != null) return cachedPath;

        log.info("Resolving protoc-gen-grpc-java version 1.74.0");

        var artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        var request = new ArtifactRequest();
        request.setArtifact(artifact);

        var remoteRepositories = project.getRemotePluginRepositories();
        request.setRepositories(remoteRepositories);

        try {
            log.info("Resolving artifact {} from {}", artifact, repositorySystem);
            var result = repositorySystem.resolveArtifact(session.getRepositorySession(), request);
            var file = result.getArtifact().getFile();

            if (!file.exists()) {
                throw new RuntimeException("protoc-gen-grpc-java not found: " + file.getAbsolutePath());
            }

            // 给非 Windows 系统设置可执行权限
            if (!System.getProperty("os.name").toLowerCase().contains("win")) {
                boolean ok = file.setExecutable(true);
                log.debug("Set executable: {}, {}", ok, file.getAbsolutePath());
            }

            cachedPath = file.getAbsolutePath();
            return cachedPath;
        } catch (Exception e) {
            log.error("Error resolving protoc-gen-grpc-java: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 自动识别操作系统类型，生成 Maven classifier
     */
    private String detectOsClassifier() {
        var os = System.getProperty("os.name").toLowerCase();
        var arch = System.getProperty("os.arch").toLowerCase();

        var classifier = os.contains("win") ? "windows-x86_64"
                : os.contains("mac") ? "osx-x86_64"
                : "linux-x86_64";

        // 如果是 ARM 架构
        if (arch.contains("aarch64") || arch.contains("arm")) {
            if (classifier.startsWith("linux")) classifier = "linux-aarch_64";
            if (classifier.startsWith("osx")) classifier = "osx-aarch_64";
        }
        return classifier;
    }
}
