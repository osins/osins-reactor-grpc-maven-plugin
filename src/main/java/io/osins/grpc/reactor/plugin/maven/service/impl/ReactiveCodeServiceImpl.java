package io.osins.grpc.reactor.plugin.maven.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.osins.grpc.reactor.plugin.maven.service.ObserverToReactor;
import io.osins.grpc.reactor.plugin.maven.service.ReactiveCodeService;
import io.osins.grpc.reactor.plugin.maven.uitls.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;
import spoon.Launcher;
import spoon.reflect.code.CtBlock;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class ReactiveCodeServiceImpl implements ReactiveCodeService {
    private final String serviceName;
    private final String packageName;
    private final String channelName;
    private final String outClient;

    @Inject
    public ReactiveCodeServiceImpl(
            @Named("serviceName") String serviceName,
            @Named("packageName") String packageName,
            @Named("channelName") String channelName,
            @Named("outClient") String outClient
    ) {
        this.serviceName = serviceName;
        this.packageName = packageName;
        this.channelName = channelName;
        this.outClient = outClient;
    }

    @Override
    public void generateSpringWebfluxConfig(Launcher launcher) {
        log.info("开始生成 Spring Webflux 配置类...");
        log.info("当前包名: {}", packageName);

        var factory = launcher.getFactory();

        // 4. 遍历所有 Stub 类
        launcher.getModel().getElements(e -> e instanceof CtClass).stream()
                .map(c -> (CtClass<?>) c)
                .filter(c -> c.getSimpleName().endsWith("Grpc"))
                .forEach(clazz -> {
                    buildStubMethod(clazz, factory);
                });

        var compilationUnit = factory.CompilationUnit().getOrCreate(packageName);
        launcher.setOutputFilter(clazz -> {
            var isOut = (clazz.getQualifiedName().startsWith(packageName) && (clazz.getQualifiedName().endsWith("GrpcConfig") || clazz.getQualifiedName().endsWith("GrpcClient")));
            log.info("正在处理[{}]: {}", isOut, clazz.getQualifiedName());
            compilationUnit.addDeclaredType(clazz);
            return isOut;
        });

        launcher.prettyprint();

        log.info("GrpcClientConfig generated completed.");
    }

    public void buildStubMethod(CtClass<?> clazz, Factory factory) {
        var qualifiedName = clazz.getQualifiedName();
        var simpleName = clazz.getSimpleName();

        log.info("添加Stub导入: {}", qualifiedName);

        var configClass = createConfigClass(clazz, factory);
        var channel = "reactor" + Strings.firstToUpperCase(Strings.firstToUpperCase(channelName));
        clazz.getMethods()
                .stream().filter(m -> m.getSimpleName().startsWith("new") && m.getSimpleName().endsWith("Stub"))
                .forEach(method -> {
                    log.info("添加Stub方法: {}", method.getSimpleName());

                    var stubType = factory.Type().createReference(io.grpc.stub.AbstractStub.class);
                    var futureStubType = factory.Type().createReference(io.grpc.stub.AbstractFutureStub.class);
                    var blockStubType = factory.Type().createReference(io.grpc.stub.AbstractBlockingStub.class);
                    var asyncStubType = factory.Type().createReference(io.grpc.stub.AbstractAsyncStub.class);

                    var methodReturnType = method.getType();
                    var methodName = "newStub";
                    if (methodReturnType.isSubtypeOf(stubType))
                        methodName = "newStub";
                    if (methodReturnType.isSubtypeOf(futureStubType))
                        methodName = "newFutureStub";
                    else if (methodReturnType.isSubtypeOf(blockStubType))
                        methodName = "newBlockingStub";
                    else if (methodReturnType.isSubtypeOf(asyncStubType))
                        methodName = "newAsyncStub";
                    else
                        return;

                    var paramType = createManagedChannelType(factory);
                    var param = factory.Core().createParameter();
                    param.setSimpleName(channel);
                    param.setType(paramType);

                    var declaring = factory.Type().createReference("io.osins.matrix.shared.grpc.client.StubUtils");
                    var monoRef = factory.Type().createReference(Mono.class);
                    var channelParam = createManagedChannelType(factory);
                    var supplierParam = factory.Type().createReference("java.util.function.Function");

                    var executableReference = factory.Executable().createReference(
                            declaring,
                            monoRef,
                            methodName,
                            List.of(channelParam, supplierParam)
                    );

                    var channelArg = factory.Core().createParameter();
                    channelArg.setSimpleName(channel);
                    channelArg.setType(channelParam);
                    var arg1 = factory.Code().createVariableRead(channelArg.getReference(), false);

                    var typeAccess = factory.Code().createTypeAccess(clazz.getReference());
                    var execRef = factory.Executable().createReference(
                            clazz.getReference(),        // 声明类型
                            factory.Type().createReference(Object.class),              // 返回类型先随便占位
                            method.getSimpleName()             // 方法名
                    );
                    var arg2 = factory.Core().createExecutableReferenceExpression();
                    arg2.setTarget(typeAccess);
                    arg2.setExecutable(execRef);

                    var expr = factory.Code().createInvocation(
                            factory.Code().createTypeAccess(declaring),
                            executableReference,
                            arg1, arg2
                    );
                    var returnStmt = factory.Code().createCtReturn(expr);
                    var statement = factory.Code().createCtBlock(returnStmt);

                    var returnTypeName = clazz.getQualifiedName() + "." + method.getType().getSimpleName();
                    var returnType = factory.Type()
                            .createReference(Mono.class)
                            .<CtTypeReference<?>>setActualTypeArguments(
                                    List.of(factory.Type().createReference(returnTypeName))
                            );

                    log.info("返回类型: {}", returnTypeName);

                    addMethod(factory, configClass, Strings.firstToLowerCase(clazz.getSimpleName().replaceAll("Service", "")+method.getSimpleName().replaceAll("new", "")) , param, statement, returnType);
                });

        ObserverToReactor.builder(factory, clazz, packageName, outClient).build();
    }

    private static CtTypeReference<?> createManagedChannelType(Factory factory) {
        return factory.Type()
                .createReference(Mono.class)
                .setActualTypeArguments(
                        List.of(factory.Type().createReference("io.grpc.ManagedChannel"))
                );
    }

    private static void addMethod(Factory factory, CtClass<?> target, String simpleName, CtParameter<Object> param, CtBlock<?> statement, CtTypeReference<?> returnType) {
        var methodName = "reactor" + Strings.firstToUpperCase(simpleName);
        var method = factory.Method().create(
                target,
                new HashSet<>(List.of(ModifierKind.PUBLIC)),
                returnType,
                methodName,
                List.of(),
                Set.of()
        );

        method.addAnnotation(factory.createAnnotation(factory.Type().createReference(Bean.class)));

        if (param != null) {
            method.addParameter(param);
        }

        method.setBody(statement);
    }

    private <T> CtClass<T> createConfigClass(CtClass<?> clazz, Factory factory) {
        // 1. 创建 GrpcClientConfig 类
        var configName = packageName + ".config." + clazz.getSimpleName() + "Config";
        var configClass = factory.Class().<T>create(configName);
        configClass.addModifier(ModifierKind.PUBLIC);

        // 添加 @Slf4j 注解
        factory.Annotation().annotate(configClass, factory.Type().createReference(Slf4j.class));

        // 添加 @Configuration 注解
        factory.Annotation().annotate(configClass, org.springframework.context.annotation.Configuration.class);

        // 添加 @RequiredArgsConstructor 注解
        factory.Annotation().annotate(configClass, factory.Type().createReference(RequiredArgsConstructor.class));

        // 添加 private final ReactiveGrpcServiceDiscovery 字段
//        var field = factory.Field().create(
//                configClass,
//                java.util.EnumSet.of(ModifierKind.PRIVATE, ModifierKind.FINAL),
//                factory.Type().createReference("io.osins.matrix.shared.grpc.client.ReactiveGrpcServiceDiscovery"),
//                "reactiveGrpcServiceDiscovery",
//                null
//        );
//        configClass.addField(field);
//
//        log.info("已添加配置类: {}", configName);
//
//        var statementStr = """
//                return reactiveGrpcServiceDiscovery.getChannel("matrix-user")
//                                .doOnNext(channel -> log.info("User service gRPC channel created successfully"))
//                                .doOnError(error -> log.error("Failed to create user service gRPC channel", error))
//                                .cache()
//                """;
//        var statement = factory.Code().createCtBlock(factory.Code().createCodeSnippetStatement(statementStr));
//
//        var returnType = factory.Type()
//                .createReference(Mono.class)
//                .<CtTypeReference<?>>setActualTypeArguments(
//                        List.of(factory.Type().createReference("io.grpc.ManagedChannel"))
//                );
//
//        addMethod(factory, configClass, Strings.firstToUpperCase(clazz.getSimpleName() + "Channel"), null, statement, returnType);

        // 2. 创建编译单元
//        var compilationUnit = factory.CompilationUnit().getOrCreate(packageName + ".generated");
//        compilationUnit.addDeclaredType(configClass);

//        var importStatement = factory.createImport(factory.Type().createReference(clazz.getQualifiedName()));
//        clazz.getPosition().getCompilationUnit().getImports().add(importStatement);

//        log.info("已添加基础导入，当前导入数量: {}", compilationUnit.getImports().size());

        return configClass;
    }

}