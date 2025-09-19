package io.osins.grpc.reactor.plugin.maven;

import io.osins.grpc.reactor.plugin.maven.service.ObserverToReactor;
import io.osins.grpc.reactor.plugin.maven.uitls.GrpcConfigCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.ModifierKind;

import java.util.Set;

@Slf4j
public class SpoonTest {
    @Test
    public void test() {
        // 创建 Launcher
        Launcher launcher = new Launcher();

        launcher.addInputResource("/home/richard/codes/matrix/matrix-shared/matrix-shared-grpc/matrix-shared-grpc-base/target/matrix-shared-grpc-base-0.0.1.jar");
        launcher.addInputResource("/home/richard/codes/matrix/matrix-auth/matrix-auth-grpc-proto/target/generated-sources/grpc/sources/java/club/hm/matrix/auth/grpc");
        launcher.addInputResource("/home/richard/codes/matrix/matrix-auth/matrix-auth-grpc-proto/target/generated-sources/grpc/sources/grpc-java/club/hm/matrix/auth/grpc/UserAuthorityServiceGrpc.java");
        launcher.getEnvironment().setNoClasspath(true); // 避免依赖问题
        launcher.getEnvironment().setAutoImports(true);

        launcher.buildModel(); // 可以没有真实资源
        launcher.setSourceOutputDirectory("/tmp");

        var factory = launcher.getFactory();
        var code = factory.Code();

        // 创建类
//        var clazz = factory.Class().create("Test");
//        clazz.setSimpleName("Test");
//
//        // 创建方法
//        var method = factory.Method().create(
//                clazz,
//                Set.of(ModifierKind.PUBLIC),
//                factory.Code().createCtTypeReference(Mono.class),
//                "newStub",
//                java.util.Collections.emptyList(),
//                java.util.Collections.emptySet()
//        );
//
//        method.setBody(factory.Code().createCtBlock(code.createCodeSnippetStatement("return null")));
//
//        // 把方法加到类
//        clazz.addMethod(method);

        var grpcClass = factory.Class().get("club.hm.matrix.auth.grpc.UserAuthorityServiceGrpc");
        var observer = ObserverToReactor.builder(factory, grpcClass, "com.example","/src/test/resources/protobuf/client").build();

        GrpcConfigCodeUtils.createBlock(factory, grpcClass, "channel");

        // 创建一个虚拟 CompilationUnit 并添加类
        var compUnit = factory.CompilationUnit().getOrCreate("com.example");
        compUnit.addDeclaredType(observer.getClientClass());

        log.info(compUnit.prettyprint());
    }
}
