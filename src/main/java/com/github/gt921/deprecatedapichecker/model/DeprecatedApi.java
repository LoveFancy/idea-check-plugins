package com.github.gt921.deprecatedapichecker.model;

public class DeprecatedApi {
    private String className;
    private String methodName;

    public DeprecatedApi() {
    }

    public DeprecatedApi(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeprecatedApi that = (DeprecatedApi) o;
        return className.equals(that.className) && methodName.equals(that.methodName);
    }

    @Override
    public int hashCode() {
        return 31 * className.hashCode() + methodName.hashCode();
    }
} 