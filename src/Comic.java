import java.util.ArrayList;
import java.util.LinkedList;

// TOOD Implement chapter list
public class Comic {
    private String name;
    private String status;
    private ArrayList<String> authors = new ArrayList<>();
    private String url;
    private String description;
    private int countedIssues;
    private LinkedList chapterUrls = new LinkedList();
    private LinkedList issues = new LinkedList<>();

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

    public int getCountedIssues() {
        return countedIssues;
    }

    public LinkedList getIssues() {
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
        countedIssues = 0;
    }

    public Comic(String name, String status, String author, String url, String description, int countedIssues, LinkedList chapterUrls, LinkedList issues){
        this.name = name;
        this.status = status;
        this.authors.add(author);
        this.url = url;
        this.description = description;
        this.countedIssues = countedIssues;
        this.chapterUrls = chapterUrls;
        this.issues = issues;
    }

    public Comic(String name, String status, String[] authors, String url, String description, int countedIssues, LinkedList chapterUrls, LinkedList issues){
        this.name = name;
        this.status = status;
        this.url = url;
        this.description = description;
        this.countedIssues = countedIssues;
        this.chapterUrls = chapterUrls;
        this.issues = issues;

        for(String author : authors){
            this.authors.add(author);
        }
    }

    public String toString(){
        return "Name: " + name +
                "\nStatus: " + status +
                "\nAuthors: " + authors +
                "\nDescription: " + description +
                "\nNumber of Issues: " + countedIssues +
                "\nURL: " + url +
                "\nChapter URLs: " + chapterUrls +
                "\nIssue Numbers: " + issues;
    }
}
