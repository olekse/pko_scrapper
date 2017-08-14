package scrapper;

public class ScrapperDemo {

    private IPKOScrapper scrapper;

    public ScrapperDemo(){
        scrapper = new IPKOScrapper();
    }

    public static void main(String[] args)  {
        try {
            new ScrapperDemo().run();
        } catch (WrongPasswordException | WrongAccountNumberException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void run(){
        scrapper.authorise("client number", "password");
        scrapper.fetchAccountList().stream().forEach(System.out::println);
    }




}
