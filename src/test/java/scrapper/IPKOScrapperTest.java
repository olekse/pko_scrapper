package scrapper;

import exception.WrongAccountNumberException;
import exception.WrongPasswordException;
import org.junit.Test;

public class IPKOScrapperTest {

    IPKOScrapper scrapper = null;

    @Test(expected = WrongAccountNumberException.class)
    public void auth_wrong_login_exception() throws WrongAccountNumberException {
        scrapper = new IPKOScrapper();
        scrapper.authenticate("23052385353", "artnartn");
    }

    @Test(expected = WrongPasswordException.class)
    public void auth_wrong_password_exception() throws WrongPasswordException {
        scrapper = new IPKOScrapper();
        scrapper.authenticate("58139759", "aerhaertaer");
    }

}
