import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.zip.*;

public class comicInformation {
    // TODO Implement cross-platform functionality
    // TODO Implement save states

    public static void main(String[] args) throws IOException{
        comicInformationGrabber();
    }

    private static void comicInformationGrabber() throws IOException{
        String url = JOptionPane.showInputDialog("Enter the URL of a comic you want to download.");

        // Establish connection to the comic webpage.
        Document webpage = Jsoup.connect(url).get();

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
        /*  Some books have multiple authors and we account for that here by splitting them.*/
        String[] authors = rawAuthors.split(", ");

            /*  We are making efficient use of memory by reusing our old variables instead of creating new ones. Now we
                move on to the description of the comic and store that. */
        comicElements = webpage.body().select("div.detail-desc-content").select("p");
        String description = comicElements.text();

        /*  Again reusing. Accessing the list of comics with the URLs and issue numbers now. */
        comicElements = webpage.body().select("ul.basic-list").select("a.ch-name");

        LinkedList<String> chapterUrls = new LinkedList<>(); // Using a Linked List to take advantage of the addFirst method.
        int issues = 0;

        /*  We are adding each element from comicElements that match a absolute URL (hence the abs:href tag) to our
            chapterUrls LinkedList. We also increment issues as each sucessful add is a new issue. */
        for(Element element : comicElements){
            chapterUrls.addFirst(element.attr("abs:href"));
            issues++;
        }

        /* TODO I created a Comic class to store the information as my goal is to implement a database of some sorts
            in the future, whether through SQL or using JSON files. */
        Comic comic = new Comic(name, status, authors, url, description, issues, chapterUrls);

        comicDownloader(comic);
    }

    private static void comicDownloader(Comic comic) throws IOException{
        /*  We can use a relative path here as the compiler knows we want to work in the project root. */
        File folder = new File("downloads\\" + comic.getName());


        if(!folder.exists()){ // If the folder doesn't exist, make the directory.
            folder.mkdir();
        }


        for(int issue = 1; issue <= comic.getIssues(); issue++){ // Download each issue
            chapterDownloader(comic.getName(), folder.getAbsolutePath(), (String) comic.getChapterUrls().get(issue - 1), issue);
        }
    }

    private static void chapterDownloader(String name, String parentFolder, String url, int issue) throws IOException{
        /*  Create a folder for each issue in the parent folder. cbzFolder is a check for our if statement further down. */
        File folder = new File(parentFolder + "\\" + name + " #" + issue);
        File cbzFolder = new File(folder.getAbsolutePath() + ".cbz");

        /*  We only want to go through the process of downloading images if the folder or the .cbz file does not exist,
            as we don't want to waste unnecessary data redownloading images already present. */
        if(!folder.exists() && !cbzFolder.exists()) {
            folder.mkdir();

            Document webpage = Jsoup.connect(url).get();

            /*  We find the number of pages by inspecting the HTML in a web browser. We found it under the
                <div class="label>, hence the .select() query. For some reason, it returns two of the same exact value,
                so we only choose one by using .first(). We want the text, as we want a numeric value. We then use
                .replaceAll() with the "\\D+" regex to replace anything that is NOT a number with an empty space. This way
                we can use Integer.parseInt() to grab the numeric value. */
            int numberOfPages = Integer.parseInt(webpage.select("div.label").first().text().replaceAll("\\D+", ""));

            /*  Download each page. Because the link on the website doesn't have a "/1" for the first page, we have
                an if statement to take care of that and it's subsequent pages. */
            for (int page = 1; page <= numberOfPages; page++) {
                if (page == 1) pageDownloader(url, folder.getAbsolutePath(), page);
                else pageDownloader(url + "/" + page, folder.getAbsolutePath(), page);
            }

            comicMaker(name, issue, folder, parentFolder); // Convert to .cbz
        }
    }

    private static void pageDownloader(String url, String folder, int page) throws IOException{
        Document webpage = Jsoup.connect(url).get();

        /*  Find the main image, which is our comic page, by the "main_img" id. We then want the URL so we can download
            the image later on, and we do that by using the .asbUrl() method so we get a whole URL. The attributeKey "src"
            identifies the URL. */
        String imageSrc = webpage.getElementById("main_img").absUrl("src");

        URL imageUrl = new URL(imageSrc); // Create a URL object out of our string URL.

        /*  We open the imageUrl as a URLConnection object because I encountered a 403 error when connecting without it.
            Upon further research, the server throws that error because it COULD happen, not because it DID happen. This
            takes care of that by allowing us to imitate a web browser, which we achieve by injecting a User Agent. */
        URLConnection image = imageUrl.openConnection();
        image.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");

        InputStream in = image.getInputStream();

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
    }

    private static void comicMaker(String name, int issue, File folder, String parentFolder) throws IOException{
        /*  We can only zip files, not a directory, so we want to get a list of all files in the directory and store them
            in a File[] array as that is what .listFiles() returns. */
        File[] pages = folder.listFiles();

        /*  We can zip right to a .cbz file as a .zip file is basically a renamed .cbz file. */
        String outputFile = name + " #" + issue + ".cbz";

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
