package com.spendly.dto;

public class DemoDataResponse {
    private boolean success;
    private String message;
    private int transactionsGenerated;
    private long demoTransactionsCount;
    private long realTransactionsCount;
    private String targetAccount;

    public DemoDataResponse() {
    }

    public DemoDataResponse(boolean success, String message, int transactionsGenerated, long demoTransactionsCount, long realTransactionsCount, String targetAccount) {
        this.success = success;
        this.message = message;
        this.transactionsGenerated = transactionsGenerated;
        this.demoTransactionsCount = demoTransactionsCount;
        this.realTransactionsCount = realTransactionsCount;
        this.targetAccount = targetAccount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTransactionsGenerated() {
        return transactionsGenerated;
    }

    public void setTransactionsGenerated(int transactionsGenerated) {
        this.transactionsGenerated = transactionsGenerated;
    }

    public long getDemoTransactionsCount() {
        return demoTransactionsCount;
    }

    public void setDemoTransactionsCount(long demoTransactionsCount) {
        this.demoTransactionsCount = demoTransactionsCount;
    }

    public long getRealTransactionsCount() {
        return realTransactionsCount;
    }

    public void setRealTransactionsCount(long realTransactionsCount) {
        this.realTransactionsCount = realTransactionsCount;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        this.targetAccount = targetAccount;
    }
}
