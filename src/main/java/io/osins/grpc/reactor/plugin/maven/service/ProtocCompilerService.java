package io.osins.grpc.reactor.plugin.maven.service;

import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 增强版 Protoc 编译服务
 */
@Slf4j
public class ProtocCompilerService {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    /**
     * 编译 .proto 文件
     */
    public void compile(ProtocCompileRequest request) throws IOException, InterruptedException, TimeoutException {
        var command = new ArrayList<String>();

        var protocExec = ensureExecutable(request.getProtocExecutable());
        command.add(protocExec);

        command.add("--plugin=protoc-gen-grpc-java=" + Optional.ofNullable(request.getProtocGenGrpcJavaPath()).filter(s-> !Strings.isNullOrEmpty(s)).orElseThrow(()->new RuntimeException("protoc-gen-grpc-java path is required")));
        command.add("--proto_path=" + request.getProtoDir().toAbsolutePath());

        if (request.getDescriptorSetOut() != null) {
            command.add("--descriptor_set_out=" + request.getDescriptorSetOut().toAbsolutePath());
        }
        if (request.getJavaOutDir() != null) {
            command.add("--java_out=" + request.getJavaOutDir().toAbsolutePath());
        }
        if (request.getGrpcJavaOutDir() != null) {
            command.add("--grpc-java_out=" + request.getGrpcJavaOutDir().toAbsolutePath());
        }
        if (request.isIncludeImports()) {
            command.add("--include_imports");
        }

        // 支持多个 proto 文件
        for (var protoFile : request.getProtoFileNames()) {
            command.add(protoFile);
        }

        log.info("Executing protoc command: {}", String.join(" ", command));

        var pb = new ProcessBuilder(command);
        pb.directory(request.getProtoDir().toFile());

        var process = pb.start();

        // 处理 stdout & stderr
        var executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> logStream(process.getInputStream(), false));
        executor.submit(() -> logStream(process.getErrorStream(), true));

        var finished = process.waitFor(
                request.getTimeout() != null ? request.getTimeout().toMillis() : DEFAULT_TIMEOUT.toMillis(),
                TimeUnit.MILLISECONDS
        );

        executor.shutdown();

        if (!finished) {
            process.destroyForcibly();
            throw new TimeoutException("Protoc compilation timed out after " +
                    (request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT));
        }

        var exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Protoc failed with exit code " + exitCode);
        }
    }

    /**
     * 确保 Windows 下加 .exe
     */
    private String ensureExecutable(String protoc) {
        if (System.getProperty("os.name").toLowerCase().contains("win") && !protoc.endsWith(".exe")) {
            return protoc + ".exe";
        }
        return protoc;
    }

    /**
     * 打印进程输出流
     */
    private void logStream(java.io.InputStream inputStream, boolean isError) {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isError) {
                    log.error("[protoc] {}", line);
                } else {
                    log.info("[protoc] {}", line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading protoc output", e);
        }
    }

    /**
     * 编译参数
     */
    @Data
    @Builder
    public static class ProtocCompileRequest {
        private String protocExecutable;           // e.g. "protoc"
        private String protocGenGrpcJavaPath;      // e.g. ".../protoc-gen-grpc-java.exe"
        private Path protoDir;                     // proto 文件目录
        private List<String> protoFileNames;       // proto 文件名列表
        private Path javaOutDir;                   // java 输出目录
        private Path grpcJavaOutDir;               // grpc-java 输出目录
        private Path descriptorSetOut;             // descriptor_set_out 路径
        private boolean includeImports;            // 是否包含 imports
        private Duration timeout;                  // 编译超时（可选）
    }
}
