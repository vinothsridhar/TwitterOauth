package twitteroauthview.sample;


import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class ViewMap extends FragmentActivity
{

    private GoogleMap mMap;
    double latitude, longitude;
    Marker sourceMarker, destMarker;
    boolean isGpsEnabled, isNetworkEnabled;
    double userlat, userlng;
    String name, tweet;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewmap);


        if (getIntent().getExtras() != null)
        {
            userlat = getIntent().getExtras().getDouble("lat");
            userlng = getIntent().getExtras().getDouble("lon");
            name = getIntent().getExtras().getString("name");
            tweet = getIntent().getExtras().getString("tweet");
        }
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());
        if (status != ConnectionResult.SUCCESS)
        {
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }
        else
        {
            FragmentManager fm = this.getSupportFragmentManager();
            SupportMapFragment f = (SupportMapFragment)fm.findFragmentById(R.id.map);
            mMap = f.getMap();
            setLocation();
        }
    }


    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        super.onResume();
    }


    public void setLocation()
    {
        try
        {
            if (sourceMarker == null)
                sourceMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(userlat, userlng)));
            else
                sourceMarker.setPosition(new LatLng(userlat, userlng));
            
            sourceMarker.setTitle(name+" tweeted..");
            sourceMarker.setSnippet(tweet);
            sourceMarker.showInfoWindow();
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(userlat, userlng)));
            
            mMap.setOnMarkerClickListener(pathMarkerClickListener);
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
    }


    GoogleMap.OnMarkerClickListener pathMarkerClickListener = new OnMarkerClickListener()
    {

        public boolean onMarkerClick(Marker arg0)
        {
            // TODO Auto-generated method stub
            arg0.setTitle(name+" tweeted..");
            arg0.setSnippet(tweet);
            return false;
        }

    };

}
