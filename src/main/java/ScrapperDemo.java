import scrapper.IPKOScrapper;

public class ScrapperDemo {

    public static void main(String[] args)  {
        IPKOScrapper scrapper = new IPKOScrapper();
        scrapper.authenticate("58139759", "-- ");
        scrapper.fetchAccountList().stream().forEach(System.out::println);
    }

}
