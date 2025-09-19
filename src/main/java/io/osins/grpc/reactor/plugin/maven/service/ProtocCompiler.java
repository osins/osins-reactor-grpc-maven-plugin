package io.osins.grpc.reactor.plugin.maven.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface ProtocCompiler {
    void compile(ProtocCompilerService.ProtocCompileRequest request) throws IOException, InterruptedException, TimeoutException;
}