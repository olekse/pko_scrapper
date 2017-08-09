package scrapper;

import sun.security.pkcs.ParsingException;

import javax.naming.AuthenticationException;
import java.io.IOException;

/**
 * Created by OleksandrSerediuk on 09.08.2017.
 */
public class ScrapperDemo {

    public static void main(String[] args)  {

        IPKOScrapper scrapper = new IPKOScrapper();

        try {

            scrapper.authenticate("login", "password");
            scrapper.listAccounts().stream().forEach(System.out::println);

        } catch (ParsingException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }

    }
}
