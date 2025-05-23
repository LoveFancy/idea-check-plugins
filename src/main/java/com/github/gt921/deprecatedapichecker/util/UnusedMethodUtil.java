package com.github.gt921.deprecatedapichecker.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;

public class UnusedMethodUtil {
    /**
     * 判断方法在项目中是否无引用
     */
    public static boolean isUnusedMethod(@NotNull PsiMethod method, @NotNull Project project) {
        // 构造全局搜索范围
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        // 搜索引用
        return ReferencesSearch.search(method, scope).findFirst() == null;
    }
} 