
import scrapper.IPKOScrapper;
import util.SystemOutLogger;

public class ScrapperDemo {
    public static void main(String[] args)  {
        IPKOScrapper scrapper = new IPKOScrapper("login", "pass", new SystemOutLogger());
        scrapper.fetchAccounts().stream().forEach(System.out::println);
    }
}
