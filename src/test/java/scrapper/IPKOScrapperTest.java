package scrapper;

import exception.WrongLoginFormat;
import exception.WrongPassword;
import model.Account;
import org.junit.Test;
import util.SystemOutLogger;

import java.util.List;

public class IPKOScrapperTest {

    @Test(expected = WrongLoginFormat.class)
    public void auth_wrong_format_exception() throws WrongLoginFormat {
        IPKOScrapper scrapper = new IPKOScrapper("23052385353", "artnartn", new SystemOutLogger());
        List<Account> list = (List<Account>) scrapper.fetchAccounts();
    }

    @Test(expected = WrongPassword.class)
    public void auth_wrong_password_exception() throws WrongPassword {
        IPKOScrapper scrapper = new IPKOScrapper("00000000", "aerhaertaer", new SystemOutLogger());
        // must insert valid account number instead of "00000000" for test to work
        // I know test should be able to be run in one click btw
        List<Account> list = (List<Account>) scrapper.fetchAccounts();
    }
}
