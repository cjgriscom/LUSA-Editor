package com.quirkygaming.lusa;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

class Code {
	public static TreeMap<Integer, Code> codes = new TreeMap<>();
	
	public static Code NONE = new Code("Never", 0);
	public static Code ALL = new Code("Every Day", 2+4+8+16+32);
	public static Code MWF = new Code("MWF", 2+8+32);
	public static Code MW = new Code("MW", 2+8);
	public static Code TR = new Code("TR", 4+16);
	public static Code M = new Code("M", 2);
	public static Code T = new Code("T", 4);
	public static Code W = new Code("W", 8);
	public static Code R = new Code("R", 16);
	public static Code F = new Code("F", 32);
	public static Code Saturday = new Code("Saturday", 64);
	
	String name;
	int code;
	public Code(String name, int code) {
		this.name = name; this.code = code;
		codes.put(code, this);
	}
	public String toString() {return name;}
}

class Item {
	static ObservableList<Code> codeList = FXCollections.observableArrayList(
			Code.NONE, Code.ALL, Code.MWF, Code.MW, Code.TR, Code.M, Code.T, Code.W, Code.R, Code.F, Code.Saturday
			);
	
	static ObservableList<String> hourList = FXCollections.observableArrayList(
			" 5 AM", " 6 AM", " 7 AM", " 8 AM", " 9 AM", "10 AM", "11 AM", "12 AM",
			" 1 PM", " 2 PM", " 3 PM", " 4 PM", " 5 PM", " 6 PM",
			" 7 PM", " 8 PM", " 9 PM", "10 PM", "11 PM", "12 PM"
			);
	
	static ObservableList<Integer> minuteList = FXCollections.observableArrayList(
			0,5,10,15,20,25,30,35,40,45,50,55
			);
	
	ObjectProperty<Code> dayCode = new SimpleObjectProperty<>();
	DoubleProperty start = new SimpleDoubleProperty(0);
	DoubleProperty end = new SimpleDoubleProperty(0);
	StringProperty room = new SimpleStringProperty();
	StringProperty name = new SimpleStringProperty();
	
	public Item(Code dayCode, double start, double end, String room, String name) {
		this.dayCode.set(dayCode);
		this.start.set(start);
		this.end.set(end);
		this.room.set(room);
		this.name.set(name);
	}
	
	public String toString() {
		return name.get() + ":" + dayCode.get().name;
	}
	
	public GridPane getPanel() {
		GridPane rep = new GridPane();
		
		ChoiceBox<Code> codes = new ChoiceBox<>(codeList);
		codes.getSelectionModel().select(dayCode.get());
		codes.setOnAction((e)->{dayCode.set(codes.getSelectionModel().getSelectedItem()); Main.triggerUpdate();});
		
		TextField namef = new TextField(name.get());
		Bindings.bindBidirectional(namef.textProperty(), name);
		namef.setOnKeyReleased((e)->{Main.triggerUpdate();});
		
		TextField roomf = new TextField(room.get());
		Bindings.bindBidirectional(roomf.textProperty(), room);
		roomf.setOnKeyReleased((e)->{Main.triggerUpdate();});
		
		int index = 0;
		
		rep.add(new Label("Day  "  ), 0, index); rep.add(codes, 1, index++);
		rep.add(new Label("Name  " ), 0, index); rep.add(namef, 1, index++);
		rep.add(new Label("Room  " ), 0, index); rep.add(roomf, 1, index++);
		
		String name = "Start  ";
		DoubleProperty property_temp = start;
		while (true) {
			final DoubleProperty property = property_temp;
			int hour_init = ((int)Math.floor(property.doubleValue())); // Minus five for index
			int minute_init = (int) (((property.doubleValue()) - hour_init) * 12);
			
			final ChoiceBox<String> hours = new ChoiceBox<>(hourList); hours.getSelectionModel().select(hour_init - 5);
			final ChoiceBox<Integer> minutes = new ChoiceBox<>(minuteList); minutes.getSelectionModel().select(minute_init);
			
			hours.setOnAction((e -> {property.set(hours.getSelectionModel().getSelectedIndex() + 5 + (minutes.getSelectionModel().getSelectedIndex() /12d)); Main.triggerUpdate();}));
			minutes.setOnAction(hours.getOnAction());
			
			rep.add(new Label(name), 0, index); rep.add(new HBox(hours,minutes), 1, index++);
			if (property_temp == end) break;
			name = "End  ";
			property_temp = end;
		}
		
		Button remove = new Button("DELETE");
		remove.setOnAction((e) -> {Main.classes.remove(this); Main.resetClasses();});
		
		rep.add(remove, 1, index++);
		rep.add(new Separator(), 0, index);
		rep.add(new Separator(), 1, index++);
		
		rep.getColumnConstraints().addAll(new ColumnConstraints(60),new ColumnConstraints(240)); // TOTAL: 300
		
		return rep;
	}
	
	public String URLCode() {
		return dayCode.get().code + "::" + start.doubleValue() + "::" + end.doubleValue() + "::" + sanitize(room.get()) + "::" + sanitize(name.get());
	}
	
	public static HashMap<String, String> sanitizeForward = new HashMap<>();
	static {
		sanitizeForward.put("\\$", "%24");
		//sanitizeForward.put("%", "%25");
		sanitizeForward.put("\\\\n", "%0A");
		sanitizeForward.put("&", "%26");
		sanitizeForward.put(",", "%2C");
		sanitizeForward.put("/", "%2F");
		sanitizeForward.put(":", "%3A");
		sanitizeForward.put(";", "%3B");
		sanitizeForward.put("=", "%3D");
		sanitizeForward.put("\\?", "%3F");
		sanitizeForward.put(" ", "%20");
	}
	
	public static String sanitize(String in) {
		for (Entry<String, String> e : sanitizeForward.entrySet()) {
			in=in.replaceAll(e.getKey(), e.getValue());
		}
		return in;
	}
	public static String desanitize(String in) {
		for (Entry<String, String> e : sanitizeForward.entrySet()) {
			in=in.replaceAll(e.getValue(), e.getKey());
		}
		return in;
	}
	
}

public class Main extends Application {
	private static final String URL_BASE = "https://webtools.letu.edu/lusa/print.php?sem=2016FA&trad=non&classes=";
	
	static GridPane master = new GridPane();
	
	static boolean update = true;
	
	public static void triggerUpdate() {
		update = true;
	}
	
	static Item CHAPEL() {return new Item(Code.MWF, 10+5d/6, 11.5, "Belcher", "Chapel");}
	
	static ObservableList<Item> classes = FXCollections.observableArrayList(CHAPEL());
	
	static GridPane classPane = new GridPane();
	
	public static void main(String[] args) {
		launch(args);
	}
	
	public static void resetClasses() {
		classPane.getChildren().clear();
		for (int i = 0; i < classes.size(); i++) {
			classPane.add(classes.get(i).getPanel(), 0, i);
		}
		triggerUpdate();
	}
	
	@Override
	public void start(Stage s) throws Exception {
		
		Button addButton = new Button("Add New Class");
		
		ScrollPane classPane_wrap =  new ScrollPane(classPane);
		master.add(new VBox(addButton, classPane_wrap), 0, 0);

		classPane_wrap.setHbarPolicy(ScrollBarPolicy.NEVER);
		classPane_wrap.setMinViewportWidth(300);
		
		addButton.setOnAction((e)->{
			classes.add(0,new Item(Code.M, 9, 10, "Room", "New Class " + classes.size()));
			resetClasses();
		});
		
		resetClasses();
		
		Button generate = new Button("Import from Pasted URL");
		TextArea url = new TextArea(); url.setWrapText(true);
		Label debug = new Label("...");
		
		generate.setOnAction((e)->{
			// Parse
			try {
				String parseme = url.getText().split("classes=")[1].trim();
				String[] items = parseme.split("~");
				ArrayList<Item> newList = new ArrayList<>();
				for (String item : items) {
					String[] parts = item.split("::");
					Item clazz = new Item(Code.codes.get(
							Integer.parseInt(parts[0])), 
							Double.parseDouble(parts[1]), 
							Double.parseDouble(parts[2]),
							Item.desanitize(parts[3]), 
							Item.desanitize(parts[4].replaceAll("\\+", "%20")));
					newList.add(clazz);
				}
				classes.clear();
				if (url.getText().contains("trad=trad")) classes.add(CHAPEL());
				classes.addAll(newList);
				debug.setText("Imported Successfully");
				resetClasses();
			} catch (Exception ex) {
				ex.printStackTrace();
				debug.setText("Could not parse pasted URL.");
			}
			
			
		});
		
		Label instructions = new Label("\nInstructions:\n\n"
				+ "To edit a schedule, right click the image in LUSA and select 'Copy Image Address'.  "
				+ "Paste it into the box above, and click 'Import from Pasted URL'.\n\n"
				+ "");
		instructions.wrapTextProperty().set(true);
		
		Button saveButton = new Button("Save as PNG");
		saveButton.setOnAction((e)->{
			FileChooser db = new FileChooser();
			db.setTitle("Select destination...");
			db.getExtensionFilters().add(new ExtensionFilter("PNG Image", "*.png"));
			File f = db.showSaveDialog(s);
			if (f != null) {
				try {
					URL website = new URL(getURL(classes));
					InputStream in = website.openStream();
					Files.copy(in, Paths.get(f.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
					in.close();
					debug.setText("Wrote to " + f.getAbsolutePath());
				} catch (Exception e1) {
					debug.setText("Error saving file: " + e1.getClass());
					e1.printStackTrace();
				}
				
			}
		});

		VBox URLPane = new VBox(generate,url,debug, new Separator(), instructions, new Separator(), new HBox(saveButton));
		master.add(URLPane, 2, 0);
		

		master.add(new Separator(), 1, 0);
		
		Scene sc = new Scene(master);
		s.setScene(sc);
		s.setTitle("LUSA Editor");
		s.setMaximized(true);
		s.show();
		
		triggerUpdate();
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (s.isShowing()) {
					if (update) Platform.runLater(new Runnable() {
						public void run() {
							String url_gen = getURL(classes);
							url.setText(url_gen);
							debug.setText("Updated");
							ImageView iv = getImage(url_gen);
							iv.fitHeightProperty().bind(master.heightProperty());
							iv.setPreserveRatio(true);
							master.add(iv, 1, 0);
							update=false;
						}
					});
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	public static String getURL(Collection<Item> classes) {
		String url = URL_BASE;
		boolean fresh = true;
		for (Item clazz : classes) {
			if (!fresh) url += "~"; // Spacer
			url += clazz.URLCode();
			fresh = false;
		}
		return url;
	}
	
	public static ImageView getImage(String url) {
		ImageView iv = new ImageView(url);
		return iv;
	}
}
