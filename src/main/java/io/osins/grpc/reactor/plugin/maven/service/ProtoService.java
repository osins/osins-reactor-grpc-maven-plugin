package io.osins.grpc.reactor.plugin.maven.service;

import com.google.protobuf.DescriptorProtos;

import java.nio.file.Path;
import java.util.List;

public interface ProtoService {
    List<DescriptorProtos.FileDescriptorSet> getFileDescriptorSet(String source, String outDesc, String outProtobuf);
    DescriptorProtos.FileDescriptorSet compileAndLoadDescriptor(Path path, String outDesc, String outProtobuf);
}
