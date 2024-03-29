package twitteroauthview.sample;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


public class MainActivity extends Activity implements OauthSuccessListener
{
    SharedPreferences pref;
    ProgressBar progress;
    ListView listView;
    Button lauth_button;
    AuthDialog dialog;
    ImageLoader imageLoader;
    List<Status> statuses;
    TextView errortext;
    int pageNumber=1;
    Twitter twitter;
    FriendsAdapter adapter;
    View mFooterView;
    LayoutInflater mInflater;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        imageLoader = new ImageLoader(this);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        mInflater=(LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        adapter=new FriendsAdapter();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        lauth_button = (Button)findViewById(R.id.button1);
        errortext = (TextView)findViewById(R.id.textView1);
        progress = (ProgressBar)findViewById(R.id.progressBar1);
        progress.setVisibility(View.GONE);
        mFooterView = mInflater.inflate(R.layout.load_more_footer, null);
        listView = (ListView)findViewById(R.id.listView1);
        listView.addFooterView(mFooterView);
        listView.setOnScrollListener(new EndlessScrollListener());
    }


    public void onClick_LaunchTwitterOAuthActivity(View button)
    {
        // this.startActivity(new Intent(this, TwitterOAuthActivity.class));
        dialog = new AuthDialog(MainActivity.this, this);
    }


    public void isSuccess(boolean isSuccess, AccessToken token)
    {
        // TODO Auto-generated method stub
        dialog.dismiss();
        if (isSuccess)
        {
            lauth_button.setVisibility(View.GONE);
            errortext.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true).setOAuthConsumerKey(Constants.CONSUMER_KEY)
                    .setOAuthConsumerSecret(Constants.CONSUMER_SECRET)
                    .setOAuthAccessToken(token.getToken()).setOAuthAccessTokenSecret(token.getTokenSecret());

            TwitterFactory tf = new TwitterFactory(cb.build());
            twitter = tf.getInstance(token);
            try
            {
                Paging paging=new Paging(pageNumber);
                statuses = twitter.getHomeTimeline(paging);
                progress.setVisibility(View.GONE);
                listView.setAdapter(adapter);
            }
            catch (TwitterException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        else
        {
            errortext.setVisibility(View.VISIBLE);
            progress.setVisibility(View.GONE);
        }
    }


    class FriendsAdapter extends BaseAdapter
    {

        public int getCount()
        {
            // TODO Auto-generated method stub
            return statuses.size();
        }


        public Object getItem(int arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }


        public long getItemId(int arg0)
        {
            // TODO Auto-generated method stub
            return 0;
        }


        public View getView(int arg0, View arg1, ViewGroup arg2)
        {
            // TODO Auto-generated method stub
            ViewHolder holder = null;
            LayoutInflater inflater =
                    (LayoutInflater)MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (arg1 == null)
            {
                arg1 = inflater.inflate(R.layout.friends_list_images, null);
                holder = new ViewHolder();
                holder.image = (ImageView)arg1.findViewById(R.id.profileimage);
                holder.text = (TextView)arg1.findViewById(R.id.textView1);
                holder.tweet = (TextView)arg1.findViewById(R.id.tweet);
                holder.time = (TextView)arg1.findViewById(R.id.time);
                holder.location=(Button)arg1.findViewById(R.id.location);
                arg1.setTag(holder);
            }
            else
                holder = (ViewHolder)arg1.getTag();

            // holder.box.setChecked(true);
            holder.text.setText(statuses.get(arg0).getUser().getName());
            holder.tweet.setText(statuses.get(arg0).getText());
            holder.location.setVisibility(View.INVISIBLE);
            if(statuses.get(arg0).getGeoLocation()!=null)
            {
                holder.location.setTag(statuses.get(arg0));
                holder.location.setVisibility(View.VISIBLE);
                holder.location.setOnClickListener(locationClickListener);
            }
            else
                holder.location.setVisibility(View.GONE);
            Date date = statuses.get(arg0).getCreatedAt();
            DateFormat df = new SimpleDateFormat("dd, MMM yy, HH:mm");
            String format = df.format(date);
            holder.time.setText(format);
            imageLoader.DisplayProfileImage(statuses.get(arg0).getUser().getProfileImageURL().toString(), holder.image);

            return arg1;
        }


        class ViewHolder
        {
            TextView text, tweet, time;
            ImageView image;
            Button location;
        }
        
        OnClickListener locationClickListener=new OnClickListener()
        {
            
            public void onClick(View v)
            {
                // TODO Auto-generated method stub
                Status s=(Status)v.getTag();
                Intent i=new Intent(MainActivity.this, ViewMap.class);
                i.putExtra("lat", s.getGeoLocation().getLatitude());
                i.putExtra("lon", s.getGeoLocation().getLongitude());
                i.putExtra("name", s.getUser().getName());
                i.putExtra("tweet", s.getText());
                
                startActivity(i);
            }
        };
    }


    public class EndlessScrollListener implements OnScrollListener
    {

        private int visibleThreshold = 5;
        private int previousTotal = 0;
        private boolean loading = true;


        public EndlessScrollListener()
        {
        }


        public EndlessScrollListener(int visibleThreshold)
        {
            this.visibleThreshold = visibleThreshold;
        }


        public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3)
        {
            // TODO Auto-generated method stub
            if (loading)
            {
                if (arg3 > previousTotal)
                {
                    loading = false;
                    previousTotal = arg3;
                    pageNumber++;
                }
            }
            if (!loading && (arg3 - arg2) <= (arg1 + visibleThreshold))
            {
                // I load the next page of gigs using a background task,
                // but you can call any function here.
                loading = true;
                new LoadingPointsHistory().execute();
            }
            
        }


        public void onScrollStateChanged(AbsListView arg0, int arg1)
        {
            // TODO Auto-generated method stub
            
        }


    }
    
    class LoadingPointsHistory extends AsyncTask<Void, Void, Void>
    {

        List<twitter4j.Status> s1;
        @Override
        protected Void doInBackground(Void... arg0)
        {
            // TODO Auto-generated method stub
            Paging paging1=new Paging(pageNumber);
            try
            {
                s1=twitter.getHomeTimeline(paging1);
            }
            catch (TwitterException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result)
        {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            statuses.addAll(s1);
            adapter.notifyDataSetChanged();
            
            if(s1.size()<20)
                listView.removeFooterView(mFooterView);
        }
        
        
    }
}
