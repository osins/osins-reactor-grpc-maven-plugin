package io.osins.grpc.reactor.plugin.maven.handler;

import lombok.Data;
import org.apache.maven.artifact.handler.ArtifactHandler;

@Data
public class ArtifactHandlerMock implements ArtifactHandler {
    private String extension;
    private String directory;
    private String classifier;
    private String packaging;
    private String language;
    private boolean includesDependencies, addedToClasspath;

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public boolean isIncludesDependencies() {
        return includesDependencies;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Deprecated
    public void setAddedToClasspath(boolean addedToClasspath) {
        this.addedToClasspath = addedToClasspath;
    }

    @Override
    @Deprecated
    public boolean isAddedToClasspath() {
        return addedToClasspath;
    }
}