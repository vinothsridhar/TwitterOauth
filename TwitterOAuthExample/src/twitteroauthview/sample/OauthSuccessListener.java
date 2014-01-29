package twitteroauthview.sample;

import twitter4j.auth.AccessToken;

public interface OauthSuccessListener
{
    public void isSuccess(boolean isSuccess,AccessToken token);
}