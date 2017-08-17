package experimental;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class SiteState {

    HtmlPage currentPage;
    Set<HtmlElement> requiredElements = new HashSet<>();

    final private void waitForPageToLoad(){
        
    }



    public void enter(){
    }




}
