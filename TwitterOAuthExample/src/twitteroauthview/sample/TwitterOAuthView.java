package twitteroauthview.sample;


import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class TwitterOAuthView extends WebView
{
    
    private static final String TAG = "TwitterOAuthView";

    private ProgressBar progress;
    private static final boolean DEBUG = false;

    public enum Result
    {
        SUCCESS,CANCELLATION,REQUEST_TOKEN_ERROR,AUTHORIZATION_ERROR,ACCESS_TOKEN_ERROR
    }


    public interface Listener
    {
        void onSuccess(TwitterOAuthView view, AccessToken accessToken);
        void onFailure(TwitterOAuthView view, Result result);
    }

    private TwitterOAuthTask twitterOAuthTask;

    private boolean cancelOnDetachedFromWindow = true;


    private boolean isDebugEnabled = DEBUG;


    public TwitterOAuthView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        // Additional initialization.
        init();
    }

    public TwitterOAuthView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        // Additional initialization.
        init();
    }


    public TwitterOAuthView(Context context)
    {
        super(context);

        // Additional initialization.
        init();
    }


    /**
     * Initialization common for all constructors.
     */
    private void init()
    {
        WebSettings settings = getSettings();

        // Not use cache.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Enable JavaScript.
        settings.setJavaScriptEnabled(true);

        // Enable zoom control.
        settings.setBuiltInZoomControls(true);

        // Scroll bar
        setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);
    }


    public void start(String consumerKey, String consumerSecret,
            String callbackUrl, boolean dummyCallbackUrl,
            Listener listener)
    {
        // Check the given arguments.
        if (consumerKey == null || consumerSecret == null || callbackUrl == null || listener == null)
        {
            throw new IllegalArgumentException();
        }

        // Convert the boolean parameter to a Boolean object to pass it
        // as an argument of AsyncTask.execute().
        Boolean dummy = Boolean.valueOf(dummyCallbackUrl);

        TwitterOAuthTask oldTask;
        TwitterOAuthTask newTask;

        synchronized (this)
        {
            // Renew Twitter OAuth task.
            oldTask = twitterOAuthTask;
            newTask = new TwitterOAuthTask();
            twitterOAuthTask = newTask;
        }

        // Cancel an old running task, if not null.
        cancelTask(oldTask);

        // Execute the new task.
        newTask.execute(consumerKey, consumerSecret, callbackUrl, dummy, listener);
    }
    
    public void setProgressBar(ProgressBar v)
    {
        progress=v;
        progress.setVisibility(View.GONE);
    }


    public void cancel()
    {
        TwitterOAuthTask task;

        synchronized (this)
        {
            // Get the reference of the running task.
            task = twitterOAuthTask;
            twitterOAuthTask = null;
        }

        // Cancel a task, if not null.
        cancelTask(task);
    }


    private void cancelTask(TwitterOAuthTask task)
    {
        // If the given argument is null.
        if (task == null)
        {
            // No task to cancel. Nothing to do.
            return;
        }

        // If the task has not been cancelled yet.
        if (task.isCancelled() == false)
        {
            if (isDebugEnabled())
            {
                Log.d(TAG, "Cancelling a task.");
            }

            // Cancel the task.
            task.cancel(true);
        }

        synchronized (task)
        {
            if (isDebugEnabled())
            {
                Log.d(TAG, "Notifying a task of cancellation.");
            }

            // Notify to interrupt the loop of waitForAuthorization().
            task.notify();
        }
    }


    public boolean isDebugEnabled()
    {
        return isDebugEnabled;
    }


    public void setDebugEnabled(boolean enabled)
    {
        isDebugEnabled = enabled;
    }


    public boolean isCancelOnDetachedFromWindow()
    {
        return cancelOnDetachedFromWindow;
    }


    public void setCancelOnDetachedFromWindow(boolean enabled)
    {
        cancelOnDetachedFromWindow = enabled;
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();

        if (isCancelOnDetachedFromWindow())
        {
            cancel();
        }
    }


    private class TwitterOAuthTask extends AsyncTask<Object, Void, Result>
    {
        private String callbackUrl;
        private boolean dummyCallbackUrl;
        private Listener listener;
        private Twitter twitter;
        private RequestToken requestToken;
        private volatile boolean authorizationDone;
        private volatile String verifier;
        private AccessToken accessToken;


        private boolean checkCancellation(String context)
        {
            if (isCancelled() == false)
            {
                return false;
            }

            if (isDebugEnabled())
            {
                Log.d(TAG, "Cancellation was detected in the context of " + context);
            }

            return true;
        }


        @Override
        protected void onPreExecute()
        {
            // Set up a WebViewClient on the UI thread.
            TwitterOAuthView.this.setWebViewClient(new LocalWebViewClient());
        }


        @Override
        protected Result doInBackground(Object... args)
        {
            // Check if this task has been cancelled.
            if (checkCancellation("doInBackground() [on entry]"))
            {
                return Result.CANCELLATION;
            }

            // Name the arguments.
            String consumerKey    = (String)  args[0];
            String consumerSecret = (String)  args[1];
            callbackUrl           = (String)  args[2];
            dummyCallbackUrl      = (Boolean) args[3];
            listener              = (Listener)args[4];

            if (isDebugEnabled())
            {
                debugDoInBackground(args);
            }

            // Create a Twitter instance with the given pair of
            // consumer key and consumer secret.
            twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(consumerKey, consumerSecret);

            // Get a request token. This triggers network access.
            requestToken = getRequestToken();
            if (requestToken == null)
            {
                // Failed to get a request token.
                return Result.REQUEST_TOKEN_ERROR;
            }

            // Access Twitter's authorization page. After the user's
            // operation, this web view is redirected to the callback
            // URL, which is caught by shouldOverrideUrlLoading() of
            // LocalWebViewClient.
            authorize();

            // Wait until the authorization step is done.
            boolean cancelled = waitForAuthorization();
            if (cancelled)
            {
                // Cancellation was detected while waiting.
                return Result.CANCELLATION;
            }

            // If the authorization has succeeded, 'verifier' is not null.
            if (verifier == null)
            {
                // The authorization failed.
                return Result.AUTHORIZATION_ERROR;
            }

            // Check if this task has been cancelled.
            if (checkCancellation("doInBackground() [before getAccessToken()]"))
            {
                return Result.CANCELLATION;
            }

            // The authorization succeeded. The last step is to get
            // an access token using the verifier.
            accessToken = getAccessToken();
            if (accessToken == null)
            {
                // Failed to get an access token.
                return Result.ACCESS_TOKEN_ERROR;
            }

            // All the steps were done successfully.
            return Result.SUCCESS;
        }


        private void debugDoInBackground(Object... args)
        {
            Log.d(TAG, "CONSUMER KEY = " + (String)args[0]);
            Log.d(TAG, "CONSUMER SECRET = " + (String)args[1]);
            Log.d(TAG, "CALLBACK URL = " + (String)args[2]);
            Log.d(TAG, "DUMMY CALLBACK URL = " + (Boolean)args[3]);

            System.setProperty("twitter4j.debug", "true");
        }


        @Override
        protected void onProgressUpdate(Void... values)
        {
            // Check if this task has been cancelled.
            if (checkCancellation("onProgressUpdate()"))
            {
                // Not load the authorization URL.
                return;
            }

            // In this implementation, onProgressUpdate() is called
            // only from authorize().

            // The authorization URL.
            String url = requestToken.getAuthorizationURL();

            if (isDebugEnabled())
            {
                Log.d(TAG, "Loading the authorization URL: " + url);
            }

            // Load the authorization URL on the UI thread.
            TwitterOAuthView.this.loadUrl(url);
        }


        @Override
        protected void onPostExecute(Result result)
        {
            if (isDebugEnabled())
            {
                Log.d(TAG, "onPostExecute: result = " + result);
            }

            if (result == null)
            {
                // Probably cancelled.
                result = Result.CANCELLATION;
            }

            if (result == Result.SUCCESS)
            {
                // Call onSuccess() method of the listener.
                fireOnSuccess();
            }
            else
            {
                // Call onFailure() method of the listener.
                fireOnFailure(result);
            }

            // Set null to TwitterOAuthView.this.twitterOAuthTask if appropriate.
            clearTaskReference();
        }


        @Override
        protected void onCancelled()
        {
            super.onCancelled();

            // Call onFailure() method of the listener.
            fireOnFailure(Result.CANCELLATION);

            // Set null to TwitterOAuthView.this.twitterOAuthTask if appropriate.
            clearTaskReference();
        }


        private void fireOnSuccess()
        {
            if (isDebugEnabled())
            {
                Log.d(TAG, "Calling Listener.onSuccess");
            }

            // Call onSuccess() method of the listener.
            listener.onSuccess(TwitterOAuthView.this, accessToken);
        }


        private void fireOnFailure(Result result)
        {
            if (isDebugEnabled())
            {
                Log.d(TAG, "Calling Listener.onFailure, result = " + result);
            }

            // Call onFailure() method of the listener.
            listener.onFailure(TwitterOAuthView.this, result);
        }


        private void clearTaskReference()
        {
            synchronized (TwitterOAuthView.this)
            {
                if (TwitterOAuthView.this.twitterOAuthTask == this)
                {
                    TwitterOAuthView.this.twitterOAuthTask = null;
                }
            }
        }


        private RequestToken getRequestToken()
        {
            try
            {
                // Get a request token. This triggers network access.
                RequestToken token = twitter.getOAuthRequestToken();

                if (isDebugEnabled())
                {
                    Log.d(TAG, "Got a request token.");
                }

                return token;
            }
            catch (TwitterException e)
            {
                // Failed to get a request token.
                e.printStackTrace();
                Log.e(TAG, "Failed to get a request token.", e);

                // No request token.
                return null;
            }
        }


        private void authorize()
        {
            // WebView.loadUrl() needs to be called on the UI thread,
            // so trigger onProgressUpdate().
            publishProgress();
        }


        private boolean waitForAuthorization()
        {
            while (authorizationDone == false)
            {
                // Check if this task has been cancelled.
                if (checkCancellation("waitForAuthorization()"))
                {
                    // Cancelled.
                    return true;
                }

                synchronized (this)
                {
                    try
                    {
                        if (isDebugEnabled())
                        {
                            Log.d(TAG, "Waiting for the authorization step to be done.");
                        }

                        // Wait until interrupted.
                        this.wait();
                    }
                    catch (InterruptedException e)
                    {
                        // Interrupted.
                        if (isDebugEnabled())
                        {
                            Log.d(TAG, "Interrupted while waiting for the authorization step to be done.");
                        }
                    }
                }
            }

            if (isDebugEnabled())
            {
                Log.d(TAG, "Finished waiting for the authorization step to be done.");
            }

            // Not cancelled.
            return false;
        }


        private void notifyAuthorization()
        {
            // The authorization step was done.
            authorizationDone = true;

            synchronized (this)
            {
                if (isDebugEnabled())
                {
                    Log.d(TAG, "Notifying that the authorization step was done.");
                }

                // Notify to interrupt the loop in waitForAuthorization().
                this.notify();
            }
        }


        private class LocalWebViewClient extends WebViewClient
        {

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
            {
                // Something wrong happened during the authorization step.
                Log.e(TAG, "onReceivedError: [" + errorCode + "] " + description);

                // Stop the authorization step.
                notifyAuthorization();
            }


            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
            {
                handler.proceed();
            }


            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                super.onPageStarted(view, url, favicon);

                progress.setVisibility(View.VISIBLE);
                // 11 = Build.VERSION_CODES.HONEYCOMB (Android 3.0)
                if (Build.VERSION.SDK_INT < 11)
                {
                    boolean stop = shouldOverrideUrlLoading(view, url);

                    if (stop)
                    {
                        // Stop loading the current page.
                        stopLoading();
                    }
                }
            }


            @Override
            public void onPageFinished(WebView view, String url)
            {
                // TODO Auto-generated method stub
                super.onPageFinished(view, url);
                progress.setVisibility(View.GONE);
            }


            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                // Check if the given URL is the callback URL.
                if (url.startsWith(callbackUrl) == false)
                {
                    // The URL is not the callback URL.
                    return false;
                }

                // This web view is about to be redirected to the callback URL.
                if (isDebugEnabled())
                {
                    Log.d(TAG, "Detected the callback URL: " + url);
                }

                // Convert String to Uri.
                Uri uri = Uri.parse(url);

                // Get the value of the query parameter "oauth_verifier".
                // A successful response should contain the parameter.
                verifier = uri.getQueryParameter("oauth_verifier");

                if (isDebugEnabled())
                {
                    Log.d(TAG, "oauth_verifier = " + verifier);
                }

                // Notify that the the authorization step was done.
                notifyAuthorization();

                // Whether the callback URL is actually accessed or not
                // depends on the value of dummyCallbackUrl. If the
                // value of dummyCallbackUrl is true, the callback URL
                // is not accessed.
                return dummyCallbackUrl;
            }
        }


        private AccessToken getAccessToken()
        {
            try
            {
                // Get an access token. This triggers network access.
                AccessToken token = twitter.getOAuthAccessToken(requestToken, verifier);

                if (isDebugEnabled())
                {
                    Log.d(TAG, "Got an access token for " + token.getScreenName());
                }

                return token;
            }
            catch (TwitterException e)
            {
                // Failed to get an access token.
                e.printStackTrace();
                Log.e(TAG, "Failed to get an access token.", e);

                // No access token.
                return null;
            }
        }
    }
}
