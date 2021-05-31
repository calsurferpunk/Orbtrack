package com.nikolaiapps.orbtrack;


import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;


public class CookieManager implements CookieJar
{
    private static final List<Cookie> allCookies = new ArrayList<>(0);
    private static final List<String> allowedDomainNames = new ArrayList<>(0);

    public CookieManager(String ... allowedDomainNames)
    {
        CookieManager.allowedDomainNames.addAll(Arrays.asList(allowedDomainNames));
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl url, @NonNull List<Cookie> cookies)
    {
        int index;
        boolean allowedDomain = false;
        String currentDomain;

        //go through each allowed domain until allowed
        for(index = 0; index < allowedDomainNames.size() && !allowedDomain; index++)
        {
            //check if in allowed domains
            allowedDomain = url.toString().toLowerCase().contains(allowedDomainNames.get(index));
        }

        //if an allowed domain
        if(allowedDomain)
        {
            //go through cookies
            for(Cookie currentCookie : cookies)
            {
                //if want to delete cookie
                if(currentCookie.value().toLowerCase().contains("delete"))
                {
                    //remember current domain
                    currentDomain = currentCookie.domain();

                    //go through cookies
                    for(Cookie currentSubCookie : allCookies)
                    {
                        //if current sub cookie in same domain
                        if(currentSubCookie.domain().contains(currentDomain))
                        {
                            //remove current sub cookie
                            allCookies.remove(currentSubCookie);
                        }
                    }
                }
                //else if cookie not in list
                else if(!allCookies.contains(currentCookie))
                {
                    //add cookie
                    allCookies.add(currentCookie);
                }
            }
        }
    }

    @Override @NonNull
    public List<Cookie> loadForRequest(@NonNull HttpUrl url)
    {
        List<Cookie> requestedCookies = new ArrayList<>(0);

        //go through cookies
        for(Cookie currentCookie : allCookies)
        {
            //if cookie matches URL
            if(currentCookie.matches(url))
            {
                //add cookie for URL
                requestedCookies.add(currentCookie);
            }
        }

        //return any requested cookies
        return(requestedCookies);
    }
}
