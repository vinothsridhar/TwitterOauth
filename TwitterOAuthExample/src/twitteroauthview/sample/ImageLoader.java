package twitteroauthview.sample;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class ImageLoader {

	MemoryCache memoryCache=new MemoryCache();
	Context context;
	private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
	ExecutorService executorService;
	Animation ani;
	Handler handler=new Handler();//handler to display images in UI thread

	public ImageLoader(Context context){
		executorService=Executors.newFixedThreadPool(5);
		ani=AnimationUtils.loadAnimation(context,R.anim.imagealpha);
		this.context=context;
	}

	final int stub_id=R.drawable.full_trans;
	public void DisplayImage(String url, ImageView imageView,boolean isAni)
	{
		Bitmap bitmap=null;
		synchronized (memoryCache) {
			imageViews.put(imageView, url);
			bitmap=memoryCache.get(url);
		}
		if(bitmap!=null)
		{
			imageView.setImageBitmap(bitmap);
		}
		else
		{
			imageView.setImageResource(stub_id);
			queuePhoto(url, imageView);
		}
	}
	
	public void DisplayProfileImage(String url, ImageView imageView)
	{
		Bitmap bitmap=null;
		synchronized (memoryCache) {
			imageViews.put(imageView, url);
			bitmap=memoryCache.get(url);
		}
		if(bitmap!=null)
		{
			imageView.setImageBitmap(bitmap);
		}
		else
		{
			imageView.setImageResource(R.drawable.user_image);
			queuePhoto(url, imageView);
		}
	}

	public void DisplayImage(String url, ImageView imageView,int id)
	{
		synchronized (memoryCache) {
			imageViews.put(imageView, url);
			Bitmap bitmap=memoryCache.get(url);
			if(bitmap!=null)
			{
				Animation ani=AnimationUtils.loadAnimation(context,R.anim.imagealpha);
				imageView.setImageBitmap(bitmap);
				//imageView.setAnimation(ani);
			}
			else
			{
				queuePhoto(url, imageView);
				imageView.setImageResource(stub_id);
			}
		}
	}

	private void queuePhoto(String url, ImageView imageView)
	{
		PhotoToLoad p=new PhotoToLoad(url, imageView);
		executorService.submit(new PhotosLoader(p));
	}

	private Bitmap getBitmap(String inputUrl)
	{
		String url = null;
		/*if(inputUrl.contains("https://graph.facebook.com"))
		{
			
		}
		else if(inputUrl.contains("https://www.google.com"))
		{
			
		}*/
		//else if (!inputUrl.contains("http://www.reviews42.com") && !inputUrl.contains("www.reviews42.com") && !inputUrl.contains("http://cdn.reviews42.com") && !inputUrl.contains("http://muneemji.review42.com"))
		if(!inputUrl.contains("http://") && !inputUrl.contains("https://"))
		{
			if(!inputUrl.contains("www"))
				inputUrl = "http://www.reviews42.com" + inputUrl;
			else
				inputUrl = "http://" + inputUrl;
		}
		//else if (!inputUrl.contains("http://"))
			//inputUrl = "http://" + inputUrl;
		
		url=inputUrl;
		Bitmap b=null;
		synchronized (memoryCache) {
			b=memoryCache.get(url);
		}
		if(b!=null)
			return b;

		Bitmap bm = null;
		InputStream is = null;
		BufferedInputStream bis = null;
		try 
		{
			HttpGet httpRequest = null;
			URL bitmapUrl = new URL(inputUrl);
			try {
				if (null!=bitmapUrl) {

					Log.e("TAG", "bitmap url"+bitmapUrl);
					httpRequest = new HttpGet(bitmapUrl.toURI());
				}

			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			HttpClient httpclient = new DefaultHttpClient();
			int statusCode;
			if (null!=httpRequest|| null!=httpclient) {
				HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
				StatusLine statusLine = response.getStatusLine();
				statusCode = statusLine.getStatusCode();
				if (statusCode==200) {
					HttpEntity entity = response.getEntity();
					BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity); 
					InputStream instream = bufHttpEntity.getContent();
					if (null!=instream) {
						bm = BitmapFactory.decodeStream(instream);	
					}

				}
			}
		}
		catch (Exception e) 
		{ e.printStackTrace(); 
		}
		finally {
			try {
				if (bis != null) 
				{bis.close();}
				if (is != null) 
				{is.close();}
			}
			catch (IOException e) 
			{ e.printStackTrace(); }
		}
		return bm;
	}

	//Task for the queue
	private class PhotoToLoad
	{
		public String url;
		public ImageView imageView;
		public PhotoToLoad(String u, ImageView i){
			url=u; 
			imageView=i;
		}
	}

	class PhotosLoader implements Runnable {
		PhotoToLoad photoToLoad;
		PhotosLoader(PhotoToLoad photoToLoad){
			this.photoToLoad=photoToLoad;
		}

		public void run() {
			try{
				if(imageViewReused(photoToLoad))
					return;
				Bitmap bmp=getBitmap(photoToLoad.url);
				synchronized (memoryCache) {
					memoryCache.put(photoToLoad.url, bmp);
				}
				if(imageViewReused(photoToLoad))
					return;
				BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad);
				handler.post(bd);
			}catch(Throwable th){
				th.printStackTrace();
			}
		}
	}

	boolean imageViewReused(PhotoToLoad photoToLoad){
		String tag=imageViews.get(photoToLoad.imageView);
		if(tag==null || !tag.equals(photoToLoad.url))
			return true;
		return false;
	}

	//Used to display bitmap in the UI thread
	class BitmapDisplayer implements Runnable
	{
		Bitmap bitmap;
		PhotoToLoad photoToLoad;
		public BitmapDisplayer(Bitmap b, PhotoToLoad p){bitmap=b;photoToLoad=p;}
		public void run()
		{
			if(imageViewReused(photoToLoad))
				return;
			if(bitmap!=null)
			{
				photoToLoad.imageView.post(new Runnable() {
					
					public void run() {
						// TODO Auto-generated method stub
						photoToLoad.imageView.setImageBitmap(bitmap);
						//photoToLoad.imageView.setAnimation(ani);
					}
				});
			}
		}
	}

	public void clearCache() {
		memoryCache.clear();
		//fileCache.clear();
	}

}
