package io.osins.grpc.reactor.plugin.maven.service;

import spoon.Launcher;

import java.io.IOException;

public interface JavaService {
    Launcher loadJavaCodes(String source) throws IOException;
}
