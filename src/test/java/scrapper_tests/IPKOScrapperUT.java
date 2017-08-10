package scrapper_tests;


import org.junit.Before;
import org.junit.Test;
import scrapper.IPKOScrapper;

import javax.naming.AuthenticationException;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;

/**
 * Created by OleksandrSerediuk on 10.08.2017.
 */
public class IPKOScrapperUT {

    IPKOScrapper scrapper = null;

    @Before
    public void before(){
        scrapper = new IPKOScrapper();
    }

    @Test(expected = FailedLoginException.class)
    public void auth_wrong_login_exception() throws FailedLoginException, IOException {
        scrapper.authorise("23052385353", "artnartn");
    }

    @Test(expected = FailedLoginException.class)
    public void auth_wrong_password_exception() throws FailedLoginException, IOException {
        scrapper.authorise("58139759", "aerhaertaer");
    }

}
