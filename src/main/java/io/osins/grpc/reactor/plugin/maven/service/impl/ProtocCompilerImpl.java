package io.osins.grpc.reactor.plugin.maven.service.impl;

import io.osins.grpc.reactor.plugin.maven.service.ProtocCompiler;
import io.osins.grpc.reactor.plugin.maven.service.ProtocCompilerService;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ProtocCompilerImpl extends ProtocCompilerService implements ProtocCompiler {
    // 直接继承增强版实现即可
    @Override
    public void compile(ProtocCompileRequest request) throws IOException, InterruptedException, TimeoutException {
        super.compile(request);
    }
}