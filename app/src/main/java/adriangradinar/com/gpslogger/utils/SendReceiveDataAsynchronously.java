package adriangradinar.com.gpslogger.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by adriangradinar on 18/02/2014.
 */
public abstract class SendReceiveDataAsynchronously extends AsyncTask<String, String, String> {

    private static final String TAG = SendReceiveDataAsynchronously.class.getSimpleName();
    protected StringBuilder sb;
    protected String result;
    protected InputStream is;
    protected MySendReceiveDataAsynchronouslyListener listener;

    final public void setListener(MySendReceiveDataAsynchronouslyListener listener) {
        this.listener = listener;
    }

    @Override
    protected String doInBackground(String... params) {

        long lengthOfContent = 0;

        try {
//            HttpClient client = new DefaultHttpClient();
//            HttpPost post = new HttpPost(params[0]);
//
//            post.setHeader("Content-type", "application/json");
//            StringEntity se = new StringEntity(params[1]);
//            post.setEntity(se);
//            //set the response
//            HttpResponse response = client.execute(post);
//            HttpEntity entity = response.getEntity();
//            is = entity.getContent();
//            lengthOfContent = entity.getContentLength();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error connecting " + e.getMessage());
        }

        //handle the response
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            sb = new StringBuilder();
            sb.append(reader.readLine() + "\n");
            String line = "0";
            int count = 0;

            while ((line = reader.readLine()) != null) {
                count++;
                sb.append(line + "\n");
                publishProgress("" + count / lengthOfContent);
            }
            reader.close();
            is.close();
            result = sb.toString();
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error converting result " + e.toString());
            return null;
        }
    }

    @Override
    final protected void onPreExecute() {
        // common stuff
        if (listener != null)
            listener.onPreExecuteConcluded();
    }

    @Override
    protected void onProgressUpdate(String... progress) {
        super.onProgressUpdate(progress);
        if (listener != null) {
            this.listener.onProgressUpdate(progress);
        }
    }

    @Override
    protected void onPostExecute(String result) {
        if (listener != null && result != null)
            listener.onPostExecuteConcluded(result);
    }

    public interface MySendReceiveDataAsynchronouslyListener {
        void onPreExecuteConcluded();

        void onPostExecuteConcluded(String result);

        void onProgressUpdate(String[] update);
    }
}
