package io.expensetracker.data;

public class Expense {

    private final static String EXPENSE_KEY_PREFIX = "expense:";

    private String account;
    private double amount;
    private String note;
    private long timestamp;


    public Expense(String account, double amount, String note, long timestamp) {
        this.account = account;
        this.amount = amount;
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getKey(){
        return String.format("%s:%s:%d", EXPENSE_KEY_PREFIX, account, timestamp);
    }

    public String toStorageString(){
        return String.format("%s|%s|%d", amount, note, timestamp);
    }

    public static Expense fromStorageString(String key, String storageString){
        String[] parts = storageString.split("\\|");
        double amount = Double.parseDouble(parts[0]);
        String note = parts[1];
        long timestamp = Long.parseLong(parts[2]);
        String account = key.split(":")[1];
        return new Expense(account, amount, note, timestamp);
    }

    public String getAccount() {
        return account;
    }

    public double getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
