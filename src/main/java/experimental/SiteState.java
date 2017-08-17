package experimental;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.HashSet;
import java.util.Set;

public abstract class SiteState {

    HtmlPage currentPage;
    Set<HtmlElement> requiredElements = new HashSet<>();

    //wait for the page to be become ready



    private void waitForElementsToLoad(){

    }


    public void enter(){
    }




}
