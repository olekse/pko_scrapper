package scrapper;

import exception.WrongAccountNumberException;
import exception.WrongPasswordException;
import org.junit.Test;
import util.SystemOutLogger;

public class IPKOScrapperTest {



    @Test(expected = WrongAccountNumberException.class)
    public void auth_wrong_login_exception() throws WrongAccountNumberException {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        scrapper.authenticate("23052385353", "artnartn");
    }

    @Test(expected = WrongPasswordException.class)
    public void auth_wrong_password_exception() throws WrongPasswordException {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        scrapper.authenticate("58139759", "aerhaertaer");
    }

}
