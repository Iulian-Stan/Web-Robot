package ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	static String[] toExplore;
	static int agents;
	static String cacheDir;
	private MainController mainController;

	public static void main(String[] args) {
		Properties props = new Properties();
		try {
			File configFile = new File("config.properties");
			InputStream inputStream = new FileInputStream(configFile);
			props.load(inputStream);
		} catch (FileNotFoundException e) {
			System.err.println("Config file not found");
		} catch (IOException e) {
			System.err.println("Some IO exception on config load");
		}

		toExplore = props.getProperty("urls", "www.w3.org").split(" ");
		agents = Integer.parseInt(props.getProperty("agents", "1"));
		cacheDir = props.getProperty("cacheDir", "");

		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainScene.fxml"));
		Parent root = fxmlLoader.load();
		mainController = (MainController)fxmlLoader.getController();
		mainController.InitAgents(agents, toExplore, cacheDir);
		
		Scene scene = new Scene(root, 800, 600);
		stage.setTitle("WebRobot");
		stage.setScene(scene);
		stage.show();
	}

	@Override
	public void stop(){
		mainController.Stop();
	}
}