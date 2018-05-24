package GUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import Comics.Comic;
import Comics.ComicMain;

public class Controller {
    @FXML private TextField urlInput;
    @FXML private TextArea urlList;
    @FXML private TextArea outputArea;
    @FXML private TextArea comicInformation;

    Queue chapterUrls = new LinkedList();

    public void print(String text){
        outputArea.appendText(text);
    }

    public void println(String text){
        outputArea.appendText(text + "\n");
    }

    public void println(){
        outputArea.appendText("\n");
    }

    public void printlnln(String text){
        outputArea.appendText(text + "\n\n");
    }

    public void printComic(Comic comic){
        comicInformation.appendText(comic.toString() + "\n");
    }

    @FXML private void onEnter(ActionEvent event) throws IOException {
        String url = urlInput.getText();
        urlInput.clear();

        urlList.appendText(url + "\n");
        ComicMain.setController(this);
        ComicMain.consoleMain(url);
        //chapterUrls.add(urlInput.getText());
    }
}
