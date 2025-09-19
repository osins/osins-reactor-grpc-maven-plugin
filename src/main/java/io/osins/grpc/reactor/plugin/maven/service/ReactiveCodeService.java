package io.osins.grpc.reactor.plugin.maven.service;

import spoon.Launcher;

public interface ReactiveCodeService {
    void generateSpringWebfluxConfig(Launcher launcher) throws Exception;
}
