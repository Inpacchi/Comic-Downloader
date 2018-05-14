import java.util.ArrayList;
import java.util.LinkedList;

public class Comic {
    private String name;
    private String status;
    private ArrayList<String> authors = new ArrayList<>();
    private String url;
    private String description;
    private int issues;
    private LinkedList chapterUrls = new LinkedList();

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public ArrayList<String> getAuthors() {
        return authors;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public int getIssues() {
        return issues;
    }

    public LinkedList getChapterUrls() {
        return chapterUrls;
    }

    public Comic(){
        name = "";
        status = "";
        url = "";
        description = "";
        issues = 0;
    }

    public Comic(String name, String status, String author, String url, String description, int issues, LinkedList chapterUrls){
        this.name = name;
        this.status = status;
        this.authors.add(author);
        this.url = url;
        this.description = description;
        this.issues = issues;
        this.chapterUrls = chapterUrls;
    }

    public Comic(String name, String status, String[] authors, String url, String description, int issues, LinkedList chapterUrls){
        this.name = name;
        this.status = status;
        this.url = url;
        this.description = description;
        this.issues = issues;
        this.chapterUrls = chapterUrls;

        for(String author : authors){
            this.authors.add(author);
        }
    }

    public String toString(){
        return "Name: " + name +
                "\nStatus: " + status +
                "\nAuthors: " + authors +
                "\nDescription: " + description +
                "\nNumber of Issues: " + issues +
                "\nURL: " + url +
                "\nChapter URLs: " + chapterUrls;
    }
}
