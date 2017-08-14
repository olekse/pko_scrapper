package scrapper;


import org.junit.Test;

import javax.security.auth.login.FailedLoginException;
import java.io.IOException;

/**
 * Created by OleksandrSerediuk on 10.08.2017.
 */
public class IPKOScrapperTest {

    IPKOScrapper scrapper = null;

    @Test(expected = WrongAccountNumberException.class)
    public void auth_wrong_login_exception() throws WrongAccountNumberException {
        scrapper = new IPKOScrapper();
        scrapper.authorise("23052385353", "artnartn");
    }

    @Test(expected = WrongPasswordException.class)
    public void auth_wrong_password_exception() throws WrongPasswordException {
        scrapper = new IPKOScrapper();
        scrapper.authorise("58139759", "aerhaertaer");
    }

}
