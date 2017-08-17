package experimental;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.HashSet;
import java.util.Set;

public class ParsableHtmlElement {

    HtmlElement resultingElement;
    Set<String> elementClasses;
    String elementId;

    public ParsableHtmlElement(){
        elementClasses = new HashSet<>();
    }

    boolean successful;

    public String getElementDiscription(){
        //Lack of abstraction was made because I won't use anything other than class and id to detect elements and this
        //code should ever go to production or used by other people

        StringBuilder targetDescriptionBuilder = new StringBuilder();

        targetDescriptionBuilder.append("{class=");
        for(String className : elementClasses){
            targetDescriptionBuilder.append(className + ",");
        }
        targetDescriptionBuilder.deleteCharAt(targetDescriptionBuilder.length()-1);
        targetDescriptionBuilder.append("},");

        targetDescriptionBuilder.append("{id=" + elementId + "}");

        return targetDescriptionBuilder.toString();
    }

    public void addRequiredClass(String className){
        elementClasses.add(className);
    }

    public void setRequiredID(String idName){
        elementId = idName;
    }

    public HtmlElement getParsedElement(){
        if (successful == false){
            throw new FailedToParseException("Couldn't find an element with " + getElementDiscription());
        }

        return resultingElement;
    }

    public boolean isSuccessful(){
        return successful;
    }

    public void tryParseFromPage(HtmlPage page){
        resultingElement = page.getFirstByXPath("//*[contains(@class, '"  + "')]");
    }
}
