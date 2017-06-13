package cx.fam.liuxu.photolist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by liuxu on 2017/2/15.
 */



public class PhotoListAdapter extends ArrayAdapter<String> implements AbsListView.OnScrollListener{

    private Set<BitmapWorkerTask> taskset;
    private LruCache<String, Bitmap> memorycache;
    JSONObject dir;
    private int size=0;
    private JSONArray list;
    private String rootpath;
    private String rooturl="http://liuxu.fam.cx:1080/PhotoShow/listfile.php?path=";
    private String dir_img = "http://liuxu.fam.cx:1080/PhotoShow/dir.png";
    tag[] taglist;
    GridView photoview;
SimpleAdapter adapter;
    public PhotoListAdapter(Context context, int textViewResourceId, GridView photolist, JSONObject content) throws JSONException {
        super(context, textViewResourceId);
        taskset = new HashSet<BitmapWorkerTask>();
        int maxmemory=(int)Runtime.getRuntime().maxMemory();
        int cacheSize=maxmemory/8;
        memorycache=new LruCache<String, Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap){
                return bitmap.getByteCount();
            }
        };
        photoview = photolist;
        photolist.setOnScrollListener(this);
        dir=content;
        rootpath=content.getString("path");
        list = content.getJSONArray("content");
        size = list.length();

        taglist = new tag[size];
        for(int i=0; i< size; i++) {
            tag tt = new tag(rootpath, list.getJSONObject(i));
            taglist[i]=tt;
        }
    }

    public int getCount(){
        return size;
    }

    private class tag{
        private Boolean isdir;
        private String url;
        private String name;
        final String serverurl = "http://liuxu.fam.cx:1080/PhotoShow/";
        final String imgurl = serverurl + "/photo/thumb/";
        final String dir_imgurl = "dir.png";
        final String query_url = serverurl + "listfile.php?path=";

        public String getTitle(){
            return name;
        }

        public tag(String path,JSONObject json) throws JSONException {
            String rtype = json.getString("type");
            name = json.getString("name");
            if(rtype.compareTo("D")==0){
                isdir = true;
                url = query_url + path;
            }else{
                isdir=false;
                url = imgurl + path + "/";
            }
        }

        public String getImgUrl(){
            if(isdir){
                return serverurl + dir_imgurl;
            } else {
                return url +  name;
            }
        }

        public String getlink(){
            if(isdir){
                return query_url+name;
            } else {
                return url + name;
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View view;
        tag t;
        if(convertView==null)
        {
            view = LayoutInflater.from(getContext()).inflate(R.layout.imagecell,null);
        }
        else
        {
            view = convertView;
        }
        final ImageView item=(ImageView)view.findViewById(R.id.imageView2);
        String url ;//getItem(position);
            //JSONObject o = list.getJSONObject(position);
            //t = new tag(rootpath, o);
        t=taglist[position];
        item.setTag(taglist[position]);
        setImageView(t.getImgUrl(),item);

        final TextView txt= (TextView)view.findViewById(R.id.text2);
        if(txt != null) {
            txt.setText(taglist[position].getTitle());
        }
        return item;
    }

    /**
     * 给ImageView设置图片。首先从LruCache中取出图片的缓存，设置到ImageView上。如果LruCache中没有该图片的缓存，
     * 就给ImageView设置一张默认图片。
     *
     *   @param url
     *          图片的URL地址，用于作为LruCache的键。
     *   @param imageView
     *          用于显示图片的控件。
     */

    private void setImageView(String url, ImageView imageView) {
        if(url != null){
            Bitmap bitmap = getBitmapFromMemoryCache(url);
            if(bitmap != null){
                imageView.setImageBitmap(bitmap);
                return;
            }
        }
        imageView.setImageResource(R.drawable.empty_photo);

    }

    /**
     * 将一张图片存储到LruCache中。
     *
     * @param key
     *      LruCache的键，这里传入图片的URL地址。
     * @param bitmap
     *      LruCache的键，这里传入从网络上下载的Bitmap对象。
     */
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            memorycache.put(key, bitmap);
        }
    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     *
     * @param key
     *      LruCache的键，这里传入图片的URL地址。
     *
     * @return 对应传入键的Bitmap对象，或者null。
     */

    public Bitmap getBitmapFromMemoryCache(String key) {
        return memorycache.get(key);
    }


    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if(scrollState == SCROLL_STATE_IDLE) {
            loadBitmaps(view.getFirstVisiblePosition(), view.getLastVisiblePosition()-view.getFirstVisiblePosition());
        } else {
            cancelAllTasks();
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    /**
     * 加载Bitmap对象。此方法会在LruCache中检查所有屏幕中可见的ImageView的Bitmap对象，
     * 如果发现任何一个ImageView的Bitmap对象不在缓存中，就会开启异步线程去下载图片。
     *
     * @param firstVisibleItem
     *            第一个可见的ImageView的下标
     * @param visibleItemCount
     *            屏幕中总共可见的元素数
     */
    private void loadBitmaps(int firstVisibleItem, int visibleItemCount) {

        for(int i=firstVisibleItem; i< firstVisibleItem+visibleItemCount; i++){
            String url = taglist[i].getImgUrl();
            Bitmap bitmap = getBitmapFromMemoryCache(url);
            if(bitmap == null){
                BitmapWorkerTask task = new BitmapWorkerTask();
                taskset.add(task);
                task.execute(i);
            } else {
                ImageView imageview = (ImageView)photoview.findViewWithTag(taglist[i]);
                imageview.setImageBitmap(bitmap);
            }
        }
    }

    private void cancelAllTasks(){

    }

    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {

        private int position;
        @Override
        protected Bitmap doInBackground(Integer... params) {
            position = params[0];
            String url = taglist[position].getImgUrl();
            Bitmap bitmap = null;
            try {
                bitmap = downloadBitmap(url);
                if(bitmap!=null){
                    addBitmapToMemoryCache(url, bitmap);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //View v = (View)photoview.getItemAtPosition(position);
            ImageView imageview = (ImageView)photoview.findViewWithTag(taglist[position]);
            //ImageView imageview = (ImageView)v.findViewById(R.id.photolistitem);
            if(imageview !=null && bitmap != null) {
                imageview.setImageBitmap(bitmap);
            }
            taskset.remove(this);
        }

        private Bitmap downloadBitmap(String urltext) throws MalformedURLException {
            Bitmap map = null;
            HttpURLConnection con = null;
            URL url = new URL(urltext);
            try{
                con = (HttpURLConnection)url.openConnection();
                con.setConnectTimeout(5*1000);
                con.setReadTimeout(10*1000);
                con.setDoInput(true);
                con.setDoOutput(true);
                map = BitmapFactory.decodeStream(con.getInputStream());
            }catch(Exception e) {
                e.printStackTrace();
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            return map;
        }
    }
}
