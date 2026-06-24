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

    // Serialize UndoRecord to a string for storage in Redis
    public String toStorageString() {
        // Use '||' as delimiter to avoid conflicts with notes containing '|'
        return String.join("||",
            expenseKey == null ? "" : expenseKey,
            account == null ? "" : account,
            Double.toString(amount),
            Double.toString(previousBalance),
            note == null ? "" : note,
            Long.toString(timestamp),
            actionType == null ? "" : actionType.name()
        );
    }

    // Deserialize UndoRecord from a storage string
    public static UndoRecord fromStorageString(String s) {
        String[] parts = s.split("\\|\\|", -1); // -1 to include trailing empty strings
        String expenseKey = parts[0].isEmpty() ? null : parts[0];
        String account = parts[1].isEmpty() ? null : parts[1];
        double amount = Double.parseDouble(parts[2]);
        double previousBalance = Double.parseDouble(parts[3]);
        String note = parts[4];
        long timestamp = Long.parseLong(parts[5]);
        ActionType actionType = parts[6].isEmpty() ? null : ActionType.valueOf(parts[6]);
        return new UndoRecord(expenseKey, account, amount, previousBalance, note, timestamp, actionType);
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
