package scrapper;

import util.SystemOutLogger;

public class ScrapperDemo {

    private IPKOScrapper scrapper;

    public ScrapperDemo(){
        scrapper = new IPKOScrapper(new SystemOutLogger());
    }

    public static void main(String[] args)  {
        try {
            new ScrapperDemo().run();
        } catch (WrongPasswordException | WrongAccountNumberException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void run(){
        scrapper.authorise("58139759", "156489Vert!@");
        scrapper.fetchAccountList().stream().forEach(System.out::println);
    }




}
