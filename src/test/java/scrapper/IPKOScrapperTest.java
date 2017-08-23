package scrapper;

import exception.WrongAccountNumber;
import exception.WrongPassword;
import org.junit.Test;
import util.SystemOutLogger;

public class IPKOScrapperTest {



    @Test(expected = WrongAccountNumber.class)
    public void auth_wrong_login_exception() throws WrongAccountNumber {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        scrapper.authenticate("23052385353", "artnartn");
    }

    @Test(expected = WrongPassword.class)
    public void auth_wrong_password_exception() throws WrongPassword {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        // must insert valid account number instead of "00000000" for test to work
        // I know test should be able to be run in one click btw
        scrapper.authenticate("00000000", "aerhaertaer");
    }

}
