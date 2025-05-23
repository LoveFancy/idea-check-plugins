package com.github.gt921.deprecatedapichecker.model;

public class DeprecatedApi {
    private String className;
    private String methodName;
    private String lastModified;
    private String lastCalled;
    private CommitInfo lastCommit;
    private int noModifyDays;
    private int noCallDays;

    public static class CommitInfo {
        private String id;
        private String author;
        private String date;
        private String message;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

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

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }
    public String getLastCalled() { return lastCalled; }
    public void setLastCalled(String lastCalled) { this.lastCalled = lastCalled; }
    public CommitInfo getLastCommit() { return lastCommit; }
    public void setLastCommit(CommitInfo lastCommit) { this.lastCommit = lastCommit; }
    public int getNoModifyDays() { return noModifyDays; }
    public void setNoModifyDays(int noModifyDays) { this.noModifyDays = noModifyDays; }
    public int getNoCallDays() { return noCallDays; }
    public void setNoCallDays(int noCallDays) { this.noCallDays = noCallDays; }

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