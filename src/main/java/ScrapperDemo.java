
import scrapper.IPKOScrapper;
import util.SystemOutLogger;

public class ScrapperDemo {

    public static void main(String[] args)  {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        scrapper.authenticate("login here", "password here");
        scrapper.fetchAccounts().stream().forEach(System.out::println);
    }

}