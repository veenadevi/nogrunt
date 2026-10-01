package nogrunt.springboot.Controllers.service;

import nogrunt.Utilities;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TokenVerificationService {

    // VERIFY TOKEN FROM CACHE
    public boolean isTokenValid(String randomkey) {
        JSONObject user = Utilities.getUserFromCache(randomkey);
        return user != null && !user.isEmpty();
    }

    // TO FETCH USERNAME FROM CACHE USING TOKEN
    public String getUsernameByToken(String randomkey) {

        JSONObject user = Utilities.getUserFromCache(randomkey);

        if (user.containsKey("uname")) {
            return (String) user.get("uname");
        } else {
            throw new IllegalArgumentException("No 'uname' field found in the cache");
        }
    }

    // TO FETCH COMPANY ID FROM CACHE USING TOKEN
    public Integer getCompanyIdByToken(String randomKey) {

        JSONObject user = Utilities.getUserFromCache(randomKey);

        if (user.containsKey("companyid")) {
            return (Integer) user.get("companyid");
        } else {
            throw new IllegalArgumentException("No 'companyid' field found in the cache");
        }
    }

}
