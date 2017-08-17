package experimental;

import org.junit.Test;

public class ParsableHtmlElementTest {

    @Test
    public void htmlElementDescriptionCorrectness(){

        ParsableHtmlElement parsableHtmlElement =
                new ParsableHtmlElement();

        parsableHtmlElement.addRequiredClass("some_class");
        parsableHtmlElement.addRequiredClass("other_class");

        parsableHtmlElement.setRequiredID("next_button");

        System.out.println(parsableHtmlElement.getElementDiscription());


    }


}
