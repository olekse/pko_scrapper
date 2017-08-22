package demo;

import scrapper.IPKOScrapper;
import util.SystemOutLogger;

public class ScrapperDemo {

    public static void main(String[] args)  {
        IPKOScrapper scrapper = new IPKOScrapper(new SystemOutLogger());
        scrapper.authenticate("58139759", "--");
        scrapper.fetchAccountList().stream().forEach(System.out::println);
    }

}
