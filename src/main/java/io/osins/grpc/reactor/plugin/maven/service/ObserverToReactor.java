package io.osins.grpc.reactor.plugin.maven.service;

import io.osins.matrix.shared.grpc.base.utils.Observer;
import io.grpc.stub.StreamObserver;
import io.osins.grpc.reactor.plugin.maven.uitls.SpoonUtils;
import io.osins.grpc.reactor.plugin.maven.uitls.Strings;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import spoon.reflect.declaration.*;
import spoon.reflect.factory.Factory;

import java.util.*;

@Slf4j
@Data
@Accessors(chain = true)
@RequiredArgsConstructor
public class ObserverToReactor {
    final Factory factory;
    final CtClass<?> grpcClass;
    final String packageName;
    CtClass<?> clientClass;
    final String outClient;

    public static ObserverToReactor builder(Factory factory, CtClass<?> grpcClass, String packageName, String outClient) {
        return new ObserverToReactor(factory, grpcClass, packageName, outClient);
    }

    public ObserverToReactor build() {
        log.info("创建服务类: {}", grpcClass.getQualifiedName());

        var stubClass = grpcClass.getNestedTypes().stream()
                .filter(s -> s.getSimpleName().startsWith(grpcClass.getSimpleName().replaceAll("Grpc", "Stub")))
                .map(s -> (CtClass<?>) s)
                .findAny().orElseThrow();

        var clientClassName = packageName + "." + grpcClass.getSimpleName().replaceAll("(\\w+\\$|Service)", "") + "Client";
        log.info("创建客户端类名: {}", clientClassName);

        clientClass = factory.Class().create(clientClassName);
        clientClass.setModifiers(Set.of(ModifierKind.PUBLIC));

        log.info("创建客户端类: {}", clientClass.getQualifiedName());

        factory.Annotation().annotate(clientClass, factory.Type().createReference("lombok.extern.slf4j.Slf4j"));
        factory.Annotation().annotate(clientClass, factory.Type().createReference("org.springframework.stereotype.Service"));
        factory.Annotation().annotate(clientClass, factory.Type().createReference("lombok.RequiredArgsConstructor"));

        // 2️⃣ 添加字段
        var monoStubType = factory.Type().createReference(Mono.class);
        monoStubType.addActualTypeArgument(stubClass.getReference());

        var field = factory.Field().create(clientClass, Set.of(ModifierKind.PRIVATE, ModifierKind.FINAL), monoStubType, Strings.firstToLowerCase(stubClass.getSimpleName()));

        log.info("已添加字段: {}", field.getSimpleName());

        // 3️⃣ 遍历 Stub 方法
        stubClass.getMethods().forEach(method -> {
            log.info("method, {}, {}", method.getSimpleName(), method.getType());
            if (!method.getType().getSimpleName().equals("void"))
                return;

            method.getParameters().forEach(p -> {
                log.info("method param, {}, {}", p.getSimpleName(), p.getType());
            });

            log.info("client method: \n{}", lambda(field, method));

            clientClass.addMethod(lambda(field, method));
        });

        return this;
    }

    public CtMethod<?> lambda(CtField<?> field, CtMethod<?> stubMethod) {
        // 1️⃣ 创建客户端方法签名
        var clientMethod = factory.Core().createMethod();
        clientMethod.setSimpleName(stubMethod.getSimpleName());
        clientMethod.addModifier(ModifierKind.PUBLIC);

        // 返回类型 Mono<StubMethodResponse>
        var responseType = stubMethod.getParameters().stream()
                .filter(p -> p.getType().isSubtypeOf(factory.Type().get(StreamObserver.class).getReference()))
                .map(CtTypedElement::getType)
                .map(t -> t.getActualTypeArguments().getFirst())
                .findFirst()
                .orElseThrow();

        var responseMonoType = factory.Type().createReference(Mono.class);
        responseMonoType.addActualTypeArgument(responseType);

        clientMethod.setType(responseMonoType);

        // 参数列表：取 Stub 方法中 Request 类型参数
        stubMethod.getParameters().stream()
                .filter(p -> p.getType().getSimpleName().endsWith("Request"))
                .forEach(p -> {
                    var param = factory.Core().createParameter();
                    param.setSimpleName(p.getSimpleName());
                    param.setType(p.getType());

                    clientMethod.addParameter(param);
                });

        // 3️⃣ 创建 Observer.mono(request, stub::method) 调用
        // Observer 类型访问
        var observerTypeAccess = factory.Code().createTypeAccess(factory.Type().createReference(Observer.class));

        // Observer.mono 方法引用
        var observerMonoMethodRef = factory.Executable().createReference(
                factory.Type().createReference(Mono.class), // 返回类型
                factory.Type().createReference(Observer.class),                            // 声明类型
                "mono"                                     // 方法名
        );

        // Lambda 参数 stub
        var lambdaParamRef = factory.Core().createParameterReference();
        lambdaParamRef.setSimpleName("stub");

        // request 参数
        var observerMonoArg1 = clientMethod.getParameters().isEmpty() ? factory.Code().createLiteral(null) : factory.Code().createVariableRead(
                clientMethod.getParameters().getFirst().getReference(), false
        );

        var stubMethodRef = factory.Executable().createReference(
                field.getType(), // Stub 类型
                SpoonUtils.castTypeReference(responseType),                                // 返回类型
                stubMethod.getSimpleName()                   // 方法名
        );

        var observerMonoArg2 = factory.Core().createExecutableReferenceExpression();
        observerMonoArg2.setExecutable(stubMethodRef);
        observerMonoArg2.setTarget(factory.Code().createVariableRead(lambdaParamRef, false));

        // Observer.mono(...) 调用
        var observerMonoInvocation = factory.Code().createInvocation(
                observerTypeAccess,
                observerMonoMethodRef,
                observerMonoArg1,
                observerMonoArg2
        );

        var lambdaParam = factory.Core().createParameter();
        lambdaParam.setSimpleName("stub");

        var lambda = factory.Core().<Observer>createLambda();
        lambda.addParameter(lambdaParam);
        lambda.setExpression(observerMonoInvocation);

        // 5️⃣ flatMap 调用: this.stub.flatMap(lambda)
        var flatMapRef = factory.Executable().createReference(
                factory.Type().createReference(Mono.class),
                responseMonoType,
                "flatMap"
        );

        var flatMapInvocation = factory.Code().createInvocation(
                factory.Code().createVariableRead(field.getReference(), false),
                flatMapRef,
                lambda
        );

        // 6️⃣ return 语句
        var returnStmt = factory.Code().createCtReturn(flatMapInvocation);

        // 7️⃣ 构造方法体
        clientMethod.setBody(factory.Code().createCtBlock(returnStmt));

        return clientMethod;
    }

}