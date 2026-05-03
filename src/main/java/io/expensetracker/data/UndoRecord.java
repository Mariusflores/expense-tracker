package io.expensetracker.data;

public class UndoRecord {

    private String expenseKey;
    private String account;
    private double amount;
    private double previousBalance;
    private String note;
    private long timestamp;
    private ActionType actionType; // "carry" or "spent" or "balance"


    public UndoRecord(String expenseKey, String account, double amount, double previousBalance, String note, long timestamp, ActionType actionType) {
        this.expenseKey = expenseKey;
        this.account = account;
        this.amount = amount;
        this.previousBalance = previousBalance;
        this.note = note;
        this.timestamp = timestamp;
        this.actionType = actionType;
    }
        public UndoRecord(Expense expense, double previousBalance, ActionType actionType) {
        this.expenseKey = expense.getKey();
        this.account = expense.getAccount();
        this.amount = expense.getAmount();
        this.note = expense.getNote();
        this.timestamp = expense.getTimestamp();
        this.previousBalance = previousBalance;
        this.actionType = actionType;
    }

    public String getExpenseKey() {
        return expenseKey;
    }
    
    public String getAccount() {
        return account;
    }
    
    public double getAmount() {
        return amount;  
    }
    
    public double getPreviousBalance() {
        return previousBalance;
    }
    
    public String getNote() {
        return note; 
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
}
