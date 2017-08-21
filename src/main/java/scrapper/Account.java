package scrapper;

public class Account {
    private String title;
    private String IBAN;
    private String balance;

    public Account(String title, String NRB, String balance) {
        this.title = title;
        this.IBAN = NRB;
        this.balance = balance;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (title != null ? !title.equals(account.title) : account.title != null) return false;
        if (IBAN != null ? !IBAN.equals(account.IBAN) : account.IBAN != null) return false;
        return balance != null ? balance.equals(account.balance) : account.balance == null;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (IBAN != null ? IBAN.hashCode() : 0);
        result = 31 * result + (balance != null ? balance.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Account[" +
                "title='" + title + '\'' +
                ", IBAN='" + IBAN + '\'' +
                ", balance='" + balance + '\'' +
                "]";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String NRB) {
        this.IBAN = NRB;
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}
