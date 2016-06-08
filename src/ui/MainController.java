package ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import webRobot.WebAgent;
import htmlParser.HTMLPage;

public class MainController implements Initializable  {

	@FXML private WebView browser;
	@FXML private TextField urlText;
	@FXML private TextField wordsText;
	@FXML private Button controlBtn;
	@FXML private ListView<HTMLPage> listView;
	@FXML private ProgressBar progressBar;
	
	private ConcurrentLinkedQueue<String> toExplore = new ConcurrentLinkedQueue<>();
	private List<HTMLPage> data = new LinkedList<>();
	private List<WebAgent> workers = null;
	private boolean isIdle = true;
	
	protected ListProperty<HTMLPage> listProperty = new SimpleListProperty<>();
	
	public void InitAgents(int agents, String[] startLinks, String cacheDir) {
		if (workers != null)
			return;
		workers = new LinkedList<>();
		toExplore.addAll(Arrays.asList(startLinks));
		for (int i = 0; i < agents; ++i) {
			WebAgent worker = new WebAgent(toExplore, data, cacheDir);
			workers.add(worker);
			new Thread(worker).start();
		}
		isIdle = false;
	}
	
	public void Pause() {
		for (int i = 0; i < workers.size(); ++i) {
			workers.get(i).Suspend();
		}
		progressBar.setProgress(0);
	}
	
	public void Resume() {
		for (int i = 0; i < workers.size(); ++i) {
			workers.get(i).Resume();
		}
		progressBar.setProgress(-1);
	}
	
	public void Stop() {
		for (int i = 0; i < workers.size(); ++i) {
			workers.get(i).Resume();
			workers.get(i).Stop();
		}
		workers.clear();
	}

	
	public void ControlAgents() {
		controlBtn.setDisable(true);
		if (isIdle) {
			Resume();
			controlBtn.setText("Pause agents");
		}
		else {
			Pause();
			controlBtn.setText("Resume agents");
		}
		isIdle = !isIdle;
		controlBtn.setDisable(false);
	}
	
	public void OpenPage() {
		browser.getEngine().load(urlText.getText());
	}
	
	public void LoadData() {
		if (data.isEmpty())
			return;
		listProperty.set(FXCollections.observableList(data));			
	}
	
	public void FilterData() {
		List<HTMLPage> filtered = new LinkedList<>();
		String filter = wordsText.getText();
		if (filter.isEmpty())
			return;
		String[] words = filter.split("[\\W]");
		for (HTMLPage page : data) {
			Set<String> wordsList = new HashSet<>(Arrays.asList(words));
			wordsList.retainAll(page.words);
			if (!wordsList.isEmpty())
				filtered.add(page);
		}
		if (filtered.isEmpty())
			return;
		listProperty.set(FXCollections.observableList(filtered));	
	}

	public void OpenCached() {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(listView.getSelectionModel().getSelectedItem().cachedFile));
        	String content = new String(encoded, Charset.defaultCharset());
        	browser.getEngine().loadContent(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		listView.itemsProperty().bind(listProperty);	
		
		listView.setCellFactory(new Callback<ListView<HTMLPage>, ListCell<HTMLPage>>(){
	        @Override
	        public ListCell<HTMLPage> call(ListView<HTMLPage> p) {
	            ListCell<HTMLPage> cell = new ListCell<HTMLPage>(){
	                @Override
	                protected void updateItem(HTMLPage t, boolean bln) {
	                    super.updateItem(t, bln);
	                    if (t != null) {
	                        setText(t.url.toString());
	                    }
	                }

	            };
	            return cell;
	        }
	    });
		
		listView.setOnMouseClicked(new EventHandler<MouseEvent>() {
	        @Override
	        public void handle(MouseEvent event) {
	        	if (event.getButton() == MouseButton.PRIMARY)
	        		browser.getEngine().load(listView.getSelectionModel().getSelectedItem().url.toString());
	        }
	    });
		
		urlText.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent keyEvent) {
		        if (keyEvent.getCode() == KeyCode.ENTER)  {
		        	OpenPage();
		        }
		    }
		});
		
		wordsText.setOnKeyPressed(new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent keyEvent) {
		        if (keyEvent.getCode() == KeyCode.ENTER)  {
		        	FilterData();
		        }
		    }
		});
		
		progressBar.setProgress(-1);
	}
}
