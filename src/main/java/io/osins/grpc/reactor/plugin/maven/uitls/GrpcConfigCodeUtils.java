package io.osins.grpc.reactor.plugin.maven.uitls;

import io.osins.grpc.reactor.plugin.maven.service.ObserverToReactor;
import reactor.core.publisher.Mono;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;

import java.util.List;
import java.util.Set;

public class GrpcConfigCodeUtils {
    public static <T> CtMethod<T> createBlock(Factory factory, CtClass<?> clazz, String channelName) {
        var core = factory.Core();
        var code = factory.Code();

        // 1. 方法返回类型 Mono<T>
        var returnType = factory.Type()
                .createReference(Mono.class)
                .<CtTypeReference<T>>setActualTypeArguments(List.of(factory.Type().createTypeParameterReference("T")));

        // 2. 创建方法
        var method = factory.Method().create(
                clazz,
                Set.of(ModifierKind.PUBLIC),
                returnType,
                "newStub",
                List.of(),
                Set.of()
        );

        var channelParam = core.createParameter();
        channelParam.setType(factory.Type()
                .createReference(Mono.class)
                .<CtTypeReference<?>>clone()
                .setActualTypeArguments(List.of(factory.Type().createReference("io.grpc.ManagedChannel"))));
        channelParam.setSimpleName(channelName);
        method.addParameter(channelParam);

        var doOnNextExecutable = core.createExecutableReference();
        doOnNextExecutable.setSimpleName("doOnNext");

        var channelVar = core.createLocalVariable();
        channelVar.setSimpleName(channelName);
        var varRef = code.createVariableRead(channelVar.getReference(), false);
        var returnStatement = core.createReturn();
        returnStatement.setReturnedExpression(code.createInvocation(varRef, doOnNextExecutable));

        // 设置方法体
        var block = core.createBlock();
        block.addStatement(returnStatement);
        method.setBody(block);

        return method;
    }
}
