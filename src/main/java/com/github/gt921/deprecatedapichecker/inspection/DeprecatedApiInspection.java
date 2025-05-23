package com.github.gt921.deprecatedapichecker.inspection;

import com.github.gt921.deprecatedapichecker.model.DeprecatedApi;
import com.github.gt921.deprecatedapichecker.settings.DeprecatedApiSettings;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.*;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import com.github.gt921.deprecatedapichecker.util.EntryPointUtil;
import com.github.gt921.deprecatedapichecker.util.UnusedMethodUtil;
import com.github.gt921.deprecatedapichecker.service.DeprecatedApiService;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.openapi.ui.Messages;
import com.intellij.refactoring.safeDelete.SafeDeleteHandler;

import java.util.List;

public class DeprecatedApiInspection extends LocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(DeprecatedApiInspection.class);

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        DeprecatedApiService.reloadConfig(holder.getProject());
        LOG.info("Building visitor for DeprecatedApiInspection, isOnTheFly: " + isOnTheFly);
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                LOG.info("Visiting method call: " + expression.getText());
                PsiMethod method = expression.resolveMethod();
                if (method == null) {
                    LOG.info("Method resolution failed for: " + expression.getText());
                    return;
                }
                
                String className = method.getContainingClass() != null ? method.getContainingClass().getQualifiedName() : null;
                String methodName = method.getName();
                LOG.info("Checking method: " + className + "." + methodName);
                
                // 检查方法是否被 @Deprecated 注解标记
                if (method.hasAnnotation("java.lang.Deprecated")) {
                    LOG.info("Found @Deprecated annotation on method: " + className + "." + methodName);
                    holder.registerProblem(
                        expression.getMethodExpression(),
                        "使用了废弃的方法: " + className + "." + methodName,
                        ProblemHighlightType.WARNING
                    );
                    return;
                }

                // 检查方法是否在配置的废弃API列表中
                if (className == null) {
                    LOG.info("Class name is null for method: " + methodName);
                    return;
                }

                List<DeprecatedApi> deprecatedApis = DeprecatedApiSettings.getInstance(holder.getProject()).getDeprecatedApis();
                LOG.info("Checking against " + deprecatedApis.size() + " deprecated APIs");
                for (DeprecatedApi api : deprecatedApis) {
                    LOG.info("Comparing with API: " + api.getClassName() + "." + api.getMethodName());
                    if (api.getClassName().equals(className) && api.getMethodName() != null && api.getMethodName().equals(methodName)) {
                        LOG.info("Found deprecated API usage: " + className + "." + methodName);
                        holder.registerProblem(
                            expression.getMethodExpression(),
                            "使用了废弃的API: " + className + "." + methodName,
                            ProblemHighlightType.WARNING
                        );
                        break;
                    }
                }
            }

            @Override
            public void visitNewExpression(@NotNull PsiNewExpression expression) {
                LOG.info("Visiting new expression: " + expression.getText());
                PsiMethod constructor = expression.resolveConstructor();
                if (constructor == null) {
                    LOG.info("Constructor resolution failed for: " + expression.getText());
                    return;
                }

                String className = constructor.getContainingClass() != null ? constructor.getContainingClass().getQualifiedName() : null;
                LOG.info("Checking constructor for class: " + className);

                // 检查构造函数是否被 @Deprecated 注解标记
                if (constructor.hasAnnotation("java.lang.Deprecated")) {
                    LOG.info("Found @Deprecated annotation on constructor: " + className);
                    holder.registerProblem(
                        expression,
                        "使用了废弃的构造函数: " + className + ".<init>",
                        ProblemHighlightType.WARNING
                    );
                    return;
                }

                // 检查构造函数是否在配置的废弃API列表中
                if (className == null) {
                    LOG.info("Class name is null for constructor");
                    return;
                }

                List<DeprecatedApi> deprecatedApis = DeprecatedApiSettings.getInstance(holder.getProject()).getDeprecatedApis();
                LOG.info("Checking constructor against " + deprecatedApis.size() + " deprecated APIs");
                for (DeprecatedApi api : deprecatedApis) {
                    LOG.info("Comparing constructor with API: " + api.getClassName() + "." + api.getMethodName());
                    if (api.getClassName().equals(className) && "<init>".equals(api.getMethodName())) {
                        LOG.info("Found deprecated constructor usage: " + className);
                        holder.registerProblem(
                            expression,
                            "使用了废弃的构造函数: " + className + ".<init>",
                            ProblemHighlightType.WARNING
                        );
                        break;
                    }
                }

                // 检查类是否在配置的废弃API列表中
                checkClassUsage(expression, className, holder);
            }

            @Override
            public void visitReferenceExpression(@NotNull PsiReferenceExpression expression) {
                LOG.info("Visiting reference expression: " + expression.getText());
                PsiElement resolved = expression.resolve();
                if (resolved instanceof PsiField) {
                    PsiField field = (PsiField) resolved;
                    String className = field.getContainingClass() != null ? field.getContainingClass().getQualifiedName() : null;
                    String fieldName = field.getName();
                    LOG.info("Checking field: " + className + "." + fieldName);
                    
                    // 检查字段是否被 @Deprecated 注解标记
                    if (field.hasAnnotation("java.lang.Deprecated")) {
                        LOG.info("Found @Deprecated annotation on field: " + className + "." + fieldName);
                        holder.registerProblem(
                            expression,
                            "使用了废弃的字段: " + className + "." + fieldName,
                            ProblemHighlightType.WARNING
                        );
                        return;
                    }

                    // 检查字段是否在配置的废弃API列表中
                    if (className == null) {
                        LOG.info("Class name is null for field: " + fieldName);
                        return;
                    }

                    List<DeprecatedApi> deprecatedApis = DeprecatedApiSettings.getInstance(holder.getProject()).getDeprecatedApis();
                    LOG.info("Checking field against " + deprecatedApis.size() + " deprecated APIs");
                    for (DeprecatedApi api : deprecatedApis) {
                        LOG.info("Comparing field with API: " + api.getClassName() + "." + api.getMethodName());
                        if (api.getClassName().equals(className) && fieldName.equals(api.getMethodName())) {
                            LOG.info("Found deprecated field usage: " + className + "." + fieldName);
                            holder.registerProblem(
                                expression,
                                "使用了废弃的字段: " + className + "." + fieldName,
                                ProblemHighlightType.WARNING
                            );
                            break;
                        }
                    }
                } else if (resolved instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) resolved;
                    String className = psiClass.getQualifiedName();
                    LOG.info("Checking class reference: " + className);
                    if (className != null) {
                        checkClassUsage(expression, className, holder);
                    }
                }
            }

            @Override
            public void visitClass(@NotNull PsiClass aClass) {
                String className = aClass.getQualifiedName();
                LOG.info("Visiting class: " + className);
                // 检查类是否被 @Deprecated 注解标记
                if (aClass.hasAnnotation("java.lang.Deprecated")) {
                    LOG.info("Found @Deprecated annotation on class: " + className);
                    PsiElement identifier = aClass.getNameIdentifier();
                    if (identifier != null) {
                        holder.registerProblem(
                            identifier,
                            "使用了废弃的类: " + className,
                            ProblemHighlightType.WARNING
                        );
                    }
                }
            }

            @Override
            public void visitMethod(@NotNull PsiMethod method) {
                String className = method.getContainingClass() != null ? method.getContainingClass().getQualifiedName() : null;
                String methodName = method.getName();
                if (className == null) return;
                List<DeprecatedApi> deprecatedApis = DeprecatedApiSettings.getInstance(holder.getProject()).getDeprecatedApis();
                boolean isDeprecatedByConfig = false;
                for (DeprecatedApi api : deprecatedApis) {
                    if (api.getClassName().equals(className) && api.getMethodName() != null && api.getMethodName().equals(methodName)) {
                        PsiElement identifier = method.getNameIdentifier();
                        if (identifier != null) {
                            String msg = "该方法已被标记为废弃方法（根据配置）";
                            holder.registerProblem(
                                identifier,
                                msg,
                                ProblemHighlightType.WARNING,
                                new DeleteMethodQuickFix(),
                                new AnnotateDeprecatedQuickFix(),
                                new InsertLogQuickFix(className, methodName),
                                new ShowDeprecatedApiInfoQuickFix(api),
                                new SafeDeleteQuickFix()
                            );
                        }
                        isDeprecatedByConfig = true;
                        break;
                    }
                }
                // 只有不在配置中时，才检测无引用且非入口
                if (!isDeprecatedByConfig && UnusedMethodUtil.isUnusedMethod(method, holder.getProject()) && !EntryPointUtil.isEntryPoint(method)) {
                    PsiElement identifier = method.getNameIdentifier();
                    if (identifier != null) {
                        holder.registerProblem(
                            identifier,
                            "该方法在项目中无引用且不是入口方法，建议清理",
                            ProblemHighlightType.WEAK_WARNING
                        );
                    }
                }
            }

            private void checkClassUsage(PsiElement element, String className, ProblemsHolder holder) {
                LOG.info("Checking class usage: " + className);
                List<DeprecatedApi> deprecatedApis = DeprecatedApiSettings.getInstance(holder.getProject()).getDeprecatedApis();
                LOG.info("Checking class against " + deprecatedApis.size() + " deprecated APIs");
                for (DeprecatedApi api : deprecatedApis) {
                    LOG.info("Comparing class with API: " + api.getClassName() + "." + api.getMethodName());
                    if (api.getClassName().equals(className) && api.getMethodName() == null) {
                        LOG.info("Found deprecated class usage: " + className);
                        holder.registerProblem(
                            element,
                            "使用了废弃的类: " + className,
                            ProblemHighlightType.WARNING
                        );
                        break;
                    }
                }
            }
        };
    }

    @Override
    public String getDisplayName() {
        return "Deprecated API Checker";
    }

    @Override
    public String getGroupDisplayName() {
        return "Deprecated API";
    }

    @Override
    public String getShortName() {
        return "DeprecatedApiInspection";
    }

    @Override
    public String getStaticDescription() {
        return "检查代码中使用的废弃API，包括被 @Deprecated 注解标记的类、方法和字段，以及配置的废弃API列表中的API。";
    }

    @Override
    public boolean isEnabledByDefault() {
        LOG.info("Checking if inspection is enabled by default");
        return true;
    }

    private static class DeleteMethodQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "删除该废弃方法";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "删除废弃方法";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiMethod method = null;
            if (element instanceof PsiMethod) {
                method = (PsiMethod) element;
            } else {
                PsiElement parent = element.getParent();
                if (parent instanceof PsiMethod) {
                    method = (PsiMethod) parent;
                }
            }
            if (method != null && method.isValid()) {
                PsiMethod finalMethod = method;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    finalMethod.delete();
                });
            }
        }
    }

    private static class AnnotateDeprecatedQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "为该方法添加@Deprecated标识";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "添加@Deprecated标识";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiMethod method = null;
            if (element instanceof PsiMethod) {
                method = (PsiMethod) element;
            } else {
                PsiElement parent = element.getParent();
                if (parent instanceof PsiMethod) {
                    method = (PsiMethod) parent;
                }
            }
            if (method != null && method.isValid() && !method.hasAnnotation("java.lang.Deprecated")) {
                PsiMethod finalMethod = method;
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    PsiModifierList modifierList = finalMethod.getModifierList();
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                    PsiAnnotation annotation = factory.createAnnotationFromText("@Deprecated", finalMethod);
                    modifierList.addBefore(annotation, modifierList.getFirstChild());
                });
            }
        }
    }

    private static class InsertLogQuickFix implements LocalQuickFix {
        private final String className;
        private final String methodName;
        public InsertLogQuickFix(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
        @NotNull
        @Override
        public String getName() {
            return "在方法开头插入log4j日志";
        }
        @NotNull
        @Override
        public String getFamilyName() {
            return "插入log4j日志";
        }
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiMethod method = null;
            if (element instanceof PsiMethod) {
                method = (PsiMethod) element;
            } else {
                PsiElement parent = element.getParent();
                if (parent instanceof PsiMethod) {
                    method = (PsiMethod) parent;
                }
            }
            if (method != null && method.getBody() != null) {
                PsiCodeBlock body = method.getBody();
                PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
                String logText = String.format("Log.info(\"[UnusedMethodMonitor] %s.%s receive call.\");", className, methodName);
                PsiStatement logStatement = factory.createStatementFromText(logText, method);
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    body.addBefore(logStatement, body.getFirstBodyElement());
                });
            }
        }
    }

    private static class ShowDeprecatedApiInfoQuickFix implements LocalQuickFix {
        private final DeprecatedApi api;
        public ShowDeprecatedApiInfoQuickFix(DeprecatedApi api) {
            this.api = api;
        }
        @NotNull
        @Override
        public String getName() {
            return "显示废弃方法详细信息";
        }
        @NotNull
        @Override
        public String getFamilyName() {
            return "显示废弃方法详细信息";
        }
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            StringBuilder msg = new StringBuilder();
            msg.append("类: ").append(api.getClassName()).append("\n");
            msg.append("方法: ").append(api.getMethodName() != null ? api.getMethodName() : "<类本身>").append("\n");
            if (api.getNoModifyDays() > 0) {
                msg.append("累计无修改时长: ").append(api.getNoModifyDays()).append("天\n");
            }
            if (api.getNoCallDays() > 0) {
                msg.append("累计无调用时长: ").append(api.getNoCallDays()).append("天\n");
            }
            if (api.getLastCommit() != null) {
                DeprecatedApi.CommitInfo commit = api.getLastCommit();
                msg.append("最近一次提交: ")
                  .append(commit.getDate() != null ? commit.getDate() : "")
                  .append(" by ")
                  .append(commit.getAuthor() != null ? commit.getAuthor() : "")
                  .append(": ")
                  .append(commit.getMessage() != null ? commit.getMessage() : "");
                if (commit.getId() != null) {
                    msg.append(" [").append(commit.getId()).append("]");
                }
                msg.append("\n");
            }
            javax.swing.SwingUtilities.invokeLater(() -> {
                com.intellij.openapi.ui.Messages.showInfoMessage(project, msg.toString(), "废弃方法详细信息");
            });
        }
    }

    private static class SafeDeleteQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "安全删除（IDEA原生）";
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return "安全删除";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiMethod method = null;
            if (element instanceof PsiMethod) {
                method = (PsiMethod) element;
            } else {
                PsiElement parent = element.getParent();
                if (parent instanceof PsiMethod) {
                    method = (PsiMethod) parent;
                }
            }
            if (method != null && method.isValid()) {
                PsiMethod finalMethod = method;
                ApplicationManager.getApplication().invokeLater(() -> {
                    SafeDeleteHandler.invoke(project, new PsiElement[]{finalMethod}, false);
                });
            }
        }
    }
} 