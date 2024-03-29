package twitteroauthview.sample;


import java.util.List;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitteroauthview.sample.TwitterOAuthView.Result;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;


public class TwitterOAuthActivity extends Activity implements TwitterOAuthView.Listener
{
    // Replace values of the parameters below with your own.
    private static final String CONSUMER_KEY = "sjn5B5qZ5n7xuTWBAnGxXA";
    private static final String CONSUMER_SECRET = "6MXWryC7bLi0Ex1FsTaYDjhxlrLPJXydydlpjlDtwLU";
    private static final String CALLBACK_URL = "http://myskills.hostoi.com/";
    private static final boolean DUMMY_CALLBACK_URL = true;


    private TwitterOAuthView view;
    private boolean oauthStarted;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Create an instance of TwitterOAuthView.
        view = new TwitterOAuthView(this);
        view.setDebugEnabled(true);

        setContentView(view);

        oauthStarted = false;
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        if (oauthStarted)
        {
            return;
        }

        oauthStarted = true;

        view.start(CONSUMER_KEY, CONSUMER_SECRET, CALLBACK_URL, DUMMY_CALLBACK_URL, this);
    }


    public void onSuccess(TwitterOAuthView view, AccessToken accessToken)
    {
        
        showMessage("Authorized by " + accessToken.getScreenName());
        view.cancel();
        System.out.println("Token:"+accessToken.getToken());
        ConfigurationBuilder cb=new ConfigurationBuilder();
        cb.setDebugEnabled(true).setOAuthConsumerKey(CONSUMER_KEY).setOAuthConsumerSecret(CONSUMER_SECRET).setOAuthAccessToken(accessToken.getToken()).setOAuthAccessTokenSecret(accessToken.getTokenSecret());
        
        TwitterFactory tf=new TwitterFactory(cb.build());
        Twitter twitter=tf.getInstance(accessToken);
        try
        {
            List<Status> statuses=twitter.getHomeTimeline();
            for(int i=0;i<statuses.size();i++)
                System.out.println(statuses.get(i));
        }
        catch (TwitterException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    public void onFailure(TwitterOAuthView view, Result result)
    {
        // Failed to get an access token.
        showMessage("Failed due to " + result);
    }


    private void showMessage(String message)
    {
        // Show a popup message.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}