package adriangradinar.com.gpslogger.utils;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

/**
 * Created by adriangradinar on 20/02/15.
 */
public class CustomExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "TAG";

    /**
     * if(!(Thread.getDefaultUncaughtExceptionHandler() instanceof CustomExceptionHandler)) {
     * Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(fullPath));
     * }
     */

    private Thread.UncaughtExceptionHandler exceptionHandler;
    private String fullPath;

    public CustomExceptionHandler(String localPath) {
        this.exceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.fullPath = localPath;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String timestamp = String.format(Locale.UK, "%d", System.currentTimeMillis());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = timestamp + ".txt";

        if (fullPath != null) {
            writeToFile(fullPath, filename, stacktrace);
        }

        //we could also send the data to the server if there's a connection
        //maybe save to a db and send when app loads again and connection is detected

        exceptionHandler.uncaughtException(thread, ex);
    }

    /**
     * @param fullPath The full path to the location where the files should be saved
     * @param fileName The name of the file (as well as the extension eg. txt, csv etc)
     * @param msg      The message
     */

    private void writeToFile(String fullPath, String fileName, String msg) {
        try {
            File dir = new File(fullPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileOutputStream fos;
            File myFile = new File(fullPath, fileName);
            if (!myFile.exists())
                myFile.createNewFile();
            byte[] data = msg.getBytes();
            try {
                fos = new FileOutputStream(myFile, true);
                fos.write(data);
                fos.flush();
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't find the file");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "An I/O Error occurred");
            e.printStackTrace();
        }
    }
}
