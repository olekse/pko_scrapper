package model;

public class Account {
    private String title;
    private String IBAN;
    private String balance;

    public Account(String title, String IBAN, String balance) {
        this.title = title;
        if(IBAN == null || IBAN.equals("")) throw new IllegalArgumentException("IBAN can't be null or empty!");
        this.IBAN = IBAN;
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        return IBAN.equals(account.IBAN);
    }

    @Override
    public int hashCode() {
        return IBAN.hashCode();
    }

    @Override
    public String toString() {
        return "Account[" +
                "title='" + title + '\'' +
                ", IBAN='" + IBAN + '\'' +
                ", balance='" + balance + '\'' +
                "]";
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}
