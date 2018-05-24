package Comics;

import GUI.Controller;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

public class Pages {
    private static Controller guiController;

    public static void setController(Controller controller){
        guiController = controller;
    }

    public static boolean download(int numberOfPages, String fileName, String url, File folder, Object issue) throws IOException {
        for (int page = 1; page <= numberOfPages; page++){
            if(!pageDownloader(fileName, url + "/" + page, folder.getAbsolutePath(), page)){
                guiController.println(fileName + " couldn't download.\n");

                for(File image : folder.listFiles()) image.delete();
                folder.delete();

                return false;
            }
        }

        return true;
    }

    private static boolean pageDownloader(String fileName, String url, String folder, int page) throws IOException{
        Document webpage;

        try {
            webpage = Jsoup.connect(url).get();
        } catch (SocketTimeoutException e){
            guiController.println("Couldn't establish connection to the webpage. Skipping...");
            return false;
        } catch (ConnectException e){
            guiController.println("Couldn't establish connection to the webpage. Skipping...");
            return false;
        }

        /*  Find the main image, which is our comic page, by the "main_img" id. We then want the URL so we can download
            the image later on, and we do that by using the .asbUrl() method so we get a whole URL. The attributeKey "src"
            identifies the URL. */
        String imageSrc = webpage.getElementById("main_img").absUrl("src");

        URL imageUrl = new URL(imageSrc); // Create a URL object out of our string URL.

        /*  We open the imageUrl as a URLConnection object because I encountered a 403 error when connecting without it.
            Upon further research, the server throws that error because it COULD happen, not because it DID happen. This
            takes care of that by allowing us to imitate a web browser, which we achieve by injecting a User Agent.

            The response code is so we can get the error stream and see if it's null or not. If it isn't, then we want to
            print whatever is in the error stream and exit downloading not only the page, but the chapter as well. */
        HttpURLConnection image = (HttpURLConnection) imageUrl.openConnection();
        image.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        image.getResponseCode();

        InputStream in;

        if(image.getErrorStream() == null) in = image.getInputStream();
        else {
            guiController.println("Received error code: " + image.getErrorStream().read() + " for " + fileName + "\n");
            return false;
        }

        OutputStream out;

        /*  Because files are read binary-style, 12 comes after 1, even though we want 2 to come after 1. We rectify this
            by appending a 0 to any number less than 10, hence our if-else statement. */
        if(page < 10) out = new BufferedOutputStream(new FileOutputStream(folder + "\\0" + page + ".jpg"));
        else out = new BufferedOutputStream(new FileOutputStream(folder + "\\" + page + ".jpg"));

        /*  Our InputStream doesn't have a .ready() function of any sorts, so we make our own. When there is nothing else
            to read in, it will return -1. So we create a value to hold the temporary values of in.read() (but we don't
            initialize it so we can use it later on.) We also don't want to increment the value, hence the empty ";" at
            the end of the -1. Once "i = in.read()" returns -1, then we know it is done with the stream. While the loop
            is being executed, we are writing it to our file using our OutputStream that we set. */
        for(int i; (i = in.read()) != -1;) out.write(i);

        /*  We want to make sure we are closing our resources to prevent any issues that can arise from keeping them open,
            such as collecting garbage data and potentially crashing the program. */
        out.close();
        in.close();
        return true;
    }
}
