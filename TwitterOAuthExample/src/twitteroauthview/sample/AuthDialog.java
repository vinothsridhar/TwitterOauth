package twitteroauthview.sample;


import twitter4j.auth.AccessToken;
import twitteroauthview.sample.TwitterOAuthView.Result;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;


public class AuthDialog implements TwitterOAuthView.Listener
{

    Context context;
    Dialog dialog;

    private TwitterOAuthView view;
    LinearLayout relativelayout;
    ProgressBar progress;
    SharedPreferences pref;
    Editor edit;
    OauthSuccessListener listener;

    public AuthDialog(Context c, OauthSuccessListener listener)
    {
        context = c;
        this.listener=listener;
        pref=PreferenceManager.getDefaultSharedPreferences(c);
        edit=pref.edit();
        dialog = new Dialog(context, R.style.CustomDialog);// ,android.R.style.Theme_Translucent_NoTitleBar);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        view = new TwitterOAuthView(context);
        view.setDebugEnabled(true);
        dialog.setContentView(R.layout.auth_dialog);
        relativelayout=(LinearLayout)dialog.findViewById(R.id.layout);
        relativelayout.addView(view);
        progress=(ProgressBar)dialog.findViewById(R.id.progressBar);
        view.setProgressBar(progress);
        //dialog.addContentView(view, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

        view.start(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET, Constants.CALLBACK_URL, Constants.DUMMY_CALLBACK_URL, this);
        dialog.show();
        
        ((ImageButton)dialog.findViewById(R.id.close_button)).setOnClickListener(new OnClickListener()
        {
            
            public void onClick(View arg0)
            {
                // TODO Auto-generated method stub
                if(view!=null)
                    view.cancel();
                dialog.dismiss();
            }
        });

    }


    public void onSuccess(TwitterOAuthView view, AccessToken accessToken)
    {
        // TODO Auto-generated method stub
        //showMessage("Authorized by " + accessToken.getScreenName());
        System.out.println("Token:" + accessToken.getToken());
        edit.putString(Constants.ACCESS_TOKEN, accessToken.getToken());
        edit.putString(Constants.ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
        edit.commit();
        listener.isSuccess(true,accessToken);
        /*ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true).setOAuthConsumerKey(Constants.CONSUMER_KEY).setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
                .setOAuthAccessToken(accessToken.getToken()).setOAuthAccessTokenSecret(accessToken.getTokenSecret());

        TwitterFactory tf = new TwitterFactory(cb.build());
        Twitter twitter = tf.getInstance(accessToken);
        try
        {
            List<Status> statuses = twitter.getHomeTimeline();
            for (int i = 0; i < statuses.size(); i++)
                System.out.println(statuses.get(i));
        }
        catch (TwitterException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

    }


    public void onFailure(TwitterOAuthView view, Result result)
    {
        // TODO Auto-generated method stub
        //showMessage("Failed due to " + result);
        listener.isSuccess(false,null);
    }


    public void dismiss()
    {
        dialog.dismiss();
    }

}
