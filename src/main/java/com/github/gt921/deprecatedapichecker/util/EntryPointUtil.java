package com.github.gt921.deprecatedapichecker.util;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameterList;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EntryPointUtil {
    private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "org.junit.Test",
            "org.junit.jupiter.api.Test",
            "org.testng.annotations.Test"
    ));

    private static final Set<String> SPRING_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "org.springframework.beans.factory.annotation.Autowired",
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Controller",
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.scheduling.annotation.Scheduled",
            "org.springframework.context.event.EventListener",
            "javax.annotation.PostConstruct",
            "javax.annotation.PreDestroy",
            "org.springframework.context.annotation.Bean"
    ));

    private static final Set<String> DUBBO_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "com.alibaba.dubbo.config.annotation.Service",
            "org.apache.dubbo.config.annotation.Service",
            "org.apache.dubbo.config.annotation.DubboService",
            "org.apache.dubbo.config.annotation.DubboReference",
            "com.alibaba.dubbo.config.annotation.Reference"
    ));

    private static final Set<String> COMMON_INTERFACE_METHODS = new HashSet<>(Arrays.asList(
            "java.lang.Runnable.run",
            "java.util.concurrent.Callable.call",
            "java.awt.event.ActionListener.actionPerformed",
            "java.util.Comparator.compare"
    ));

    public static boolean isEntryPoint(@NotNull PsiMethod method) {
        // main方法
        if (isMainMethod(method)) return true;
        // 测试方法
        if (hasAnyAnnotation(method, TEST_ANNOTATIONS)) return true;
        // 方法上注解
        if (hasAnyAnnotation(method, SPRING_ANNOTATIONS)) return true;
        if (hasAnyAnnotation(method, DUBBO_ANNOTATIONS)) return true;
        // 类上注解
        if (isClassAnnotatedWith(method, SPRING_ANNOTATIONS)) return true;
        if (isClassAnnotatedWith(method, DUBBO_ANNOTATIONS)) return true;
        // 常见接口实现
        if (isCommonInterfaceMethod(method)) return true;
        return false;
    }

    private static boolean isMainMethod(PsiMethod method) {
        if (!"main".equals(method.getName())) return false;
        if (!method.hasModifierProperty(PsiModifier.PUBLIC) || !method.hasModifierProperty(PsiModifier.STATIC)) return false;
        if (!"void".equals(method.getReturnType() != null ? method.getReturnType().getCanonicalText() : "")) return false;
        PsiParameterList parameterList = method.getParameterList();
        if (parameterList.getParametersCount() != 1) return false;
        String paramType = parameterList.getParameters()[0].getType().getCanonicalText();
        return "java.lang.String[]".equals(paramType);
    }

    private static boolean hasAnyAnnotation(PsiMethod method, Set<String> annotationSet) {
        for (PsiAnnotation annotation : method.getModifierList().getAnnotations()) {
            String qName = annotation.getQualifiedName();
            if (qName != null && annotationSet.contains(qName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isClassAnnotatedWith(PsiMethod method, Set<String> annotationSet) {
        PsiClass clazz = method.getContainingClass();
        if (clazz == null) return false;
        PsiAnnotation[] annotations = clazz.getModifierList() != null ? clazz.getModifierList().getAnnotations() : new PsiAnnotation[0];
        for (PsiAnnotation annotation : annotations) {
            String qName = annotation.getQualifiedName();
            if (qName != null && annotationSet.contains(qName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCommonInterfaceMethod(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) return false;
        for (PsiClass iface : containingClass.getInterfaces()) {
            String ifaceName = iface.getQualifiedName();
            if (ifaceName == null) continue;
            String key = ifaceName + "." + method.getName();
            if (COMMON_INTERFACE_METHODS.contains(key)) {
                return true;
            }
        }
        return false;
    }
} 