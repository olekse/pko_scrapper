package scrapper;

import sun.security.pkcs.ParsingException;

import javax.naming.AuthenticationException;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.rmi.AccessException;

public class ScrapperDemo {

    public static void main(String[] args)  {

        IPKOScrapper scrapper = new IPKOScrapper();

        try {

            scrapper.authorise("client number", "password");
            scrapper.listAccounts().stream().forEach(System.out::println);

        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (FailedLoginException e) {
            e.printStackTrace();
        } catch (AccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
