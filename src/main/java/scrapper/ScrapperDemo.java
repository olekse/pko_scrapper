package scrapper;

import org.junit.internal.runners.statements.Fail;
import sun.security.pkcs.ParsingException;

import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.rmi.AccessException;

public class ScrapperDemo {

    public static void main(String[] args)  {
        IPKOScrapper scrapper = new IPKOScrapper();

        try {
            scrapper.authorise("client number", "password");
            scrapper.fetchAccountList().stream().forEach(System.out::println);
        } catch (WrongPasswordException | WrongAccountNumberException ex) {
            ex.printStackTrace();
        }

    }
}
