package Comics;

import GUI.Controller;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.*;

public class ComicMain extends Thread {
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    static Comic comic; // Global variable so it can be used in any method without parameter passthrough

    private static Controller guiController; // Static reference to our GUI controller so we can print to the GUI

    /*  These control our multi-threading. Because our class extends thread, that means we can create an object from our
        class and initialize it so we can do what we need to do in our main method. The processingQueue will be initialized
        in our main method as well, as a ConcurrentLinkedQueue so multiple threads can work out of it at one time. */
    private static ComicMain thread1 = new ComicMain();
    private static ComicMain thread2 = new ComicMain();
    private static ComicMain thread3 = new ComicMain();
    private static ComicMain thread4 = new ComicMain();

    static Queue processingQueue;

    static ArrayList skippedIssues; // For each issue that is skipped, keep track of it.

    private volatile boolean stop = false; // Our exit value for our threads.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO Implement cross-platform functionality
    // TODO Implement save states
    // TODO Implement a way to login to the website and check favorites for comic URLs
    // TODO Implement a way to run in the background and passively check, if so desired. Otherwise, run on launch

    public static void setController(Controller controller){ guiController = controller; }

    public static void consoleMain(String url) throws IOException{
        processingQueue = new ConcurrentLinkedQueue();
        skippedIssues = new ArrayList();

        if(!comicInformationGrabber(url)) return;
        threadStarter();
    }

    public void run(){
        if(!stop){
            try {
                comicDownloader();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void exit(){ stop = true; }
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static boolean comicInformationGrabber(String url) throws IOException{
        guiController.printlnln("Establishing connection to " + url);

        // Establish connection to the comic webpage.
        Connection webpageConnection;

        try {
            webpageConnection = Jsoup.connect(url);
            guiController.printlnln("Connection " + webpageConnection.execute().statusMessage());
        } catch (ConnectException e){
            guiController.println("Couldn't establish connection to the webpage. Try again later.\n");
            return false;
        } catch (MalformedURLException | IllegalArgumentException e) {
            guiController.println("That's not a valid URL. Exiting...");
            return false;
        }

        Document webpage = webpageConnection.get();

        /*  We only want to deal with the body of the page. On top of that, we can narrow it down to where the
            attributes are held by selecting the class box anime-desc and then selecting each individual table
             element. */
        Elements comicElements = webpage.body().select("div.anime-desc").select("tbody > tr > td");
        comicElements.select("span").remove(); // Remove unnecessary elements.

        ArrayList attributes = new ArrayList<>(); // A simpler list to store our comic book attributes in.

        /*  For each element, if the next element is not equal to null (as when we remove an object from the Elements
            as we did above, it literally removes it, but leaves a space, which is null. We don't want that in our
            simplified attributes arraylist. */
        for(Element element : comicElements) if(element.nextElementSibling() != null) attributes.add(element.nextElementSibling().text());

        attributes.remove(1); // We are not keeping track of the alternate title.
        attributes.remove(4); // Nor are we keeping track of the tags.
        attributes.remove(1); // Nor the year.

        String name = (String) attributes.get(0);
        String status = (String) attributes.get(1);
        String rawAuthors = (String) attributes.get(2);
        String[] authors = rawAuthors.split(", "); // Some books have multiple authors and we account for that here by splitting them

        /*  We are making efficient use of memory by reusing our old variables instead of creating new ones. Now we
            move on to the description of the comic and store that. */
        comicElements = webpage.body().select("div.detail-desc-content").select("p");
        String description = comicElements.text();

        LinkedList<String> chapterUrls = new LinkedList<>(); // Using a Linked List to take advantage of the addFirst method.
        LinkedList issues = new LinkedList<>();

        int countedIssues = chapterEnumerator(webpage, chapterUrls, issues);

        /*  This is our check to see if there are more chapters on different pages. */
        if(webpage.getAllElements().hasClass("general-nav")){
            comicElements = webpage.body().select("div.general-nav").get(0).getElementsByAttribute("href");
            ArrayList pages = new ArrayList();

            /*  We want to make sure we aren't including duplicate chapters. */
            for(Element element : comicElements) if(!pages.contains(element.attr("abs:href"))) pages.add(element.attr("abs:href"));

            for(Object page : pages){
                Document newWebpage = Jsoup.connect((String) page).get();

                countedIssues += chapterEnumerator(newWebpage, chapterUrls, issues);
            }
        }

        /* TODO I created a Comic class to store the information as my goal is to implement a database of some sorts
            in the future, whether through SQL or using JSON files. */
        comic = new Comic(name, status, authors, url, description, countedIssues, chapterUrls, issues);

        guiController.printComic(comic);
        return true;
    }

    private static void threadStarter(){
        LinkedList issues = comic.getIssues();

        for(Object issue : issues) {
            if(issue.toString().contains(".")) processingQueue.add(issue);
                else processingQueue.add(issue);
        }

        thread1.setName("Comic-Downloader Thread 1");
        thread2.setName("Comic-Downloader Thread 2");
        thread3.setName("Comic-Downloader Thread 3");
        thread4.setName("Comic-Downloader Thread 4");

        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();

        guiController.printlnln("Threads started for downloading!");
    }

    // TODO Review Big O Notation for LinkedList. It might be cheaper to start at the last webpage and add each issue from the bottom.
    private static int chapterEnumerator(Document webpage, LinkedList<String> chapterUrls, LinkedList issues){
        int countedIssues = 0;

        /*  Accessing the list of comics with the URLs and issue numbers now. */
        Elements urls = webpage.body().select("ul.basic-list").select("a.ch-name");

        /*  We are adding each element from urls that match a absolute URL (hence the abs:href tag) to our
            chapterUrls LinkedList. We also increment issues as each successful add is a new issue. */
        for(Element element : urls){
            String url = element.attr("abs:href");
            chapterUrls.addFirst(url);

            /*  We want to split the URL into parts as the specific part we need, the issue number, is separated by a '-'.
                If you run an index on the array, you'll find the issue number at index 4, which is all we need. */
            String[] urlParts = url.split("-");

            if(urlParts[urlParts.length - 1].contains(".")) issues.addFirst(Double.parseDouble(urlParts[4]));
                else issues.addFirst(Integer.parseInt(urlParts[urlParts.length - 1]));

            countedIssues++;
        }

        return countedIssues;
    }

    private static void comicDownloader() throws IOException{
        /*  I made it final so the variable cannot be changed no matter what, as we don't ever want our default save path
        changed. This defaults our save path to the user's documents folder, regardless of operating system. We can use
        a relative path here as the compiler knows we want to work in the project root. */
        final String DEFAULTDIRECTORY = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
        final File APPLICATIONROOT = new File(DEFAULTDIRECTORY + "\\Comic-Downloader");
        if(!APPLICATIONROOT.mkdir()) APPLICATIONROOT.mkdir();

        File folder = new File(APPLICATIONROOT.getAbsolutePath() + "\\" + comic.getName());

        if(!folder.exists()){ // If the folder doesn't exist, make the directory.
            folder.mkdir();
            //guiController.println("Creating folder " + folder + "...\n");
        }

        /*  Since we're working with a queue, our loop-through process is extremely simple. While the queue is not empty,
            poll an item (remove an item) from the top of the queue into an int issue, which we will then plug into our
            comicDownloader method. We have to make sure that we typecast our issue variable to type Integer as an Object
            type variable is stored in our processingQueue. We must also make sure we subtract 1 from the result as we are
            working with binary indexes. Each thread will do this and pull a number off the queue, hence working with a
            different issue. */
        while(!processingQueue.isEmpty()){
            if(processingQueue.isEmpty()) Thread.currentThread().interrupt();
                else {
                    int issue = ((Integer) processingQueue.poll()) - 1;
                    chapterDownloader(comic.getName(), folder.getAbsolutePath(), (String) comic.getChapterUrls().get(issue), comic.getIssues().get(issue));
                }
        }

        if(thread1.isInterrupted()) thread1.exit();
            else if(thread2.isInterrupted()) thread2.exit();
            else if(thread3.isInterrupted()) thread3.exit();
            else if(thread4.isInterrupted()) thread4.exit();

         if(Thread.activeCount() == 3){
             guiController.println("All threads closed.");
             if(skippedIssues.isEmpty()) guiController.println("All issues downloaded! Exiting application...");
                else guiController.println("Missing issues: " + skippedIssues);
         }
    }

    private static Document webpageConnection(String url, Object issue) throws IOException{
        Document webpage;

        try {
            webpage = Jsoup.connect(url).get();
        } catch (SocketTimeoutException | ConnectException e){
            guiController.println("Couldn't establish connection to the webpage. Skipping...\n");
            skippedIssues.add(issue);
            return null;
        }

        return webpage;
    }

    private static void chapterDownloader(String name, String parentFolder, String url, Object issue) throws IOException{
        /*  Create a folder for each issue in the parent folder. cbzFolder is a check for our if statement further down. */
        File folder = new File(parentFolder + "\\" + name + " #" + issue);
        File cbzFolder = new File(parentFolder + "\\" + name + " #" + issue + ".cbz");
        String fileName = name + " #" + issue;

        /*  We only want to go through the process of downloading images if the folder or the .cbz file does not exist,
            as we don't want to waste unnecessary data redownloading images already present. */
        if(!folder.exists() && !cbzFolder.exists()) {
            //guiController.println("Establishing connection to " + url + "...");
            Document webpage = webpageConnection(url, issue);

            if(webpage == null) return;

            //guiController.println("Connection established!");

            /*  We find the number of pages by inspecting the HTML in a web browser. We found it under the
                <div class="label>, hence the .select() query. For some reason, it returns two of the same exact value,
                so we only choose one by using .first(). We want the text, as we want a numeric value. We then use
                .replaceAll() with the "\\D+" regex to replace anything that is NOT a number with an empty space. This way
                we can use Integer.parseInt() to grab the numeric value. */
            int numberOfPages = Integer.parseInt(webpage.select("div.label").first().text().replaceAll("\\D+", ""));

            folder.mkdir();
            /*guiController.println("Creating folder " + folder + "...\n" +
                    "Downloading " + fileName + "...");*/
            guiController.println("Downloading " + fileName + " on " + Thread.currentThread().getName() + "...\n");

            if(!pageDownloader(numberOfPages, fileName, url, folder, issue)) return;

            comicMaker(name, issue, folder, parentFolder); // Convert to .cbz

            guiController.println("Downloaded " + fileName + "!\n");
        } else if(folder.exists()){
            //guiController.println("Establishing connection to " + url + "...");

            Document webpage = webpageConnection(url, issue);

            if(webpage == null) return;

            //guiController.println("Connection established!");

            int numberOfPages = Integer.parseInt(webpage.select("div.label").first().text().replaceAll("\\D+", ""));
            //guiController.println(fileName + " already exists!\n" + "Checking for an incomplete download...");

            File[] images = folder.listFiles();
            int counter = 0;

            for(int i = 0; i < images.length; i++) counter++;

            if(counter != numberOfPages){
                guiController.println("Incomplete download found. Deleting and redownloading " + fileName + "...");
                for(File image : images) image.delete();

                if(!pageDownloader(numberOfPages, fileName, url, folder, issue)) return;

                comicMaker(name, issue, folder, parentFolder);
                guiController.println("Downloaded " + fileName + "!\n");
            }

        } else {
            guiController.printlnln(fileName + " already downloaded!");
        }
    }

    private static boolean pageDownloader(int numberOfPages, String fileName, String url, File folder, Object issue) throws IOException{
        for (int page = 1; page <= numberOfPages; page++){
            if(!internalPageDownloader(fileName, url + "/" + page, folder.getAbsolutePath(), page)){
                guiController.println(fileName + " couldn't download.\n");

                for(File image : folder.listFiles()) image.delete();
                folder.delete();

                skippedIssues.add(issue);
                return false;
            }
        }

        return true;
    }

    private static boolean internalPageDownloader(String fileName, String url, String folder, int page) throws IOException{
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

    private static void comicMaker(String name, Object issue, File folder, String parentFolder) throws IOException{
        /*  We can only zip files, not a directory, so we want to get a list of all files in the directory and store them
            in a File[] array as that is what .listFiles() returns. */
        File[] pages = folder.listFiles();

        String outputFile = name + " #" + issue + ".cbz";
        //guiController.println("Compressing " + name + " #" + issue + " to .cbz archive...");

        /*  Our FileOutputStream is the path of our output. ZipOutputStream will zip to that file. */
        FileOutputStream fos = new FileOutputStream(parentFolder + "\\" + outputFile);
        ZipOutputStream zos = new ZipOutputStream(fos);

        /*  For each page, we want to pass it into another method rather than have everything in one method. It makes for
            better resource handling. We delete each page at the end of the execution so we can delete the folder once
            we're done zipping it. Why? Because Java doesn't allow for the deleting of folders with files in it.

            Oh, you mean why delete in the first place? Because there's no reason to have duplicate the data if we have
            the file we need. */
        for(File page : pages){
            pageWriter(page, zos);
            page.delete();
        }

        folder.delete();
        zos.close();
        fos.close();
    }

    private static void pageWriter(File page, ZipOutputStream zos) throws IOException{
        FileInputStream fis = new FileInputStream(page);

        byte[] buffer = new byte[1024]; // These values are for our compression and reading.
        int length;

        ZipEntry ze = new ZipEntry(page.getName()); // This tells our ZipOutputStream what to add to the archive.

        zos.putNextEntry(ze); // Push it to our ZipOutputStream.

        /*  We are only reading 1 kB (1024 bytes) at a time, and writing that much in our ZipOutputStream. */
        while((length = fis.read(buffer)) > 0) zos.write(buffer, 0, length);

        zos.closeEntry();
        fis.close();
    }
}
