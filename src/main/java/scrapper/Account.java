package scrapper;

public class Account {
    private String title;
    private String IBAN;
    private String balance; // shifted 2 decimal points

    public Account(String title, String NRB, String balance) {
        this.title = title;
        this.IBAN = NRB;
        this.balance = balance;
    }

    public String getTitle() {
        return title;
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
