package fi.tuni.prog3.sisu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

/**
 * Controls the main UI-window and its elements. Is responsible for program
 * functionality.
 * Controls mainGUI.fxml.
 * 
 * @author Joni Koskinen
 * @author Julius Juutilainen
 */
public class Controller implements Initializable {

    private static HashMap<String, Degree> degrees;
    private static HashMap<String, Course> allCourses;
    private static HashMap<String, DegreeModule> allModules;
    private static Student student;
    private static Degree degree;
    private static String selectedElement;
    // private static HashMap<String, Boolean> coursesDone;

    @FXML
    private TreeView<String> mainView = new TreeView<>();
    @FXML
    private WebView courseInfo = new WebView();
    @FXML
    private Text studentName = new Text();
    @FXML
    private Text studentNumber = new Text();
    @FXML
    private Button saveButton = new Button();
    @FXML
    private Button loadButton = new Button();
    @FXML
    private Button switchButton = new Button();
    @FXML
    private ComboBox<String> searchBar = new ComboBox<>();
    @FXML
    private VBox completedCourses = new VBox();
    @FXML
    private VBox selectableCourseList = new VBox();
    @FXML
    private CheckBox courseCheckBox = new CheckBox();
    @FXML
    private Text allCredits = new Text();
    @FXML
    private WebView motivation = new WebView();

    // TODO: Tarkista että paluuarvot ja heitot on dokumentoitu!

    /**
     * Initializes the UI and performs necessary actions.
     * 
     * @param arg0 unused
     * @param arg1 unused
     */
    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        degree = degrees.get(student.getDegree());

        allCourses = new HashMap<>();
        allModules = new HashMap<>();

        TreeItem<String> rootItem = GUITools.initializeTree(degree);

        GUITools.setUpDegreeBox(searchBar, degrees);
        // selectableCourseList = GUITools.initializeCheckList(degrees);

        mainView.setRoot(rootItem);

        studentNumber.setText(student.getNumber());
        studentName.setText(student.getName());

        motivation.getEngine().load(GUITools.getMotivationalImageUrl());
        refreshStudiesCompleted();

    }

    /**
     * Shows info based on selected element and toggles the visibility of "course
     * completed" checkbox.
     * Checks if clicked element is a course and sets checkbox visibility by it.
     * NOTE: Not every element has info that can be fetched.
     */
    @FXML
    public void selectItem() {
        TreeItem<String> item = mainView.getSelectionModel().getSelectedItem();
        if (item != null) {
            String courseHeader = item.getValue();
            String[] splitString = courseHeader.split(" ");
            String[] courseName = splitString;
            if(splitString.length > 1){
                courseName = Arrays.copyOf(splitString, splitString.length-1);
            }

            StringBuilder sb = new StringBuilder();

            for (String s : courseName) {
                sb.append(s).append(" ");
            }
            sb.setLength(sb.length() - 1);
            String name = sb.toString();
            setSelectedElement(name);

            Course c = searchCourse(name);

            if (c != null) {
                if (c.getContent() != null) {
                    courseInfo.getEngine().loadContent("");
                    courseInfo.getEngine().loadContent(c.getContent());
                } else if (c.getAdditional() != null) {
                    courseInfo.getEngine().loadContent("");
                    courseInfo.getEngine().loadContent(c.getAdditional());
                }

            }
            // EI toimi täysin
            DegreeModule m = searchModule(splitString[0]);
            if (m != null) {
                courseInfo.getEngine().loadContent("");
                courseInfo.getEngine().loadContent(m.getOutcomes());
            }
            // Ei toimi lainkaan
            if (name.equals(degree.getName())) {
                courseInfo.getEngine().loadContent("");
                // courseInfo.getEngine().loadContent(degree.getOutcomes());

            }

            // Checkboxin nollaus jos dataa ei tulekkaan
            courseCheckBox.setSelected(false);

            if (allCourses.containsKey(Controller.selectedElement)) {
                courseCheckBox.setVisible(true);
                if (student.getCoursesDone().get(Controller.selectedElement) == null
                        || student.getCoursesDone() == null) {
                    student.addCoursesDone(Controller.selectedElement, courseCheckBox.isSelected());
                }
                courseCheckBox.setSelected(student.getCoursesDone().get(Controller.selectedElement));
            } else {
                courseCheckBox.setVisible(false);
            }

        }
    }

    /**
     * "Completes" course
     * Gives the Student-object a course key and sets visible credits to TreeView
     * TreeItem.
     * Lastly refreshes the completed studies part of the UI view.
     */
    @FXML
    public void checkBoxOnClick(){
        
        if(Controller.selectedElement != null){
            student.addCoursesDone(Controller.selectedElement,courseCheckBox.isSelected());

            TreeItem<String> item = mainView.getSelectionModel().getSelectedItem();
            addCreditsToTree(item);
            Course c = searchCourse(combineString(splitString(item.getValue())));
            if(courseCheckBox.isSelected()){
                student.addCredits(c.getTargetCredits());
            }
            else{
                student.subtractCredits(c.getTargetCredits());
            }
            //? tarpeellinen
            courseCheckBox.setSelected(student.getCoursesDone().get(Controller.selectedElement));
            refreshStudiesCompleted();
        }
        


    }

    /**
     * Refreshes the comleted studies part of the UI.
     * Adds completed courses to a VBOX and adds credits to the allCredits Text
     * element.
     */
    @FXML
    public void refreshStudiesCompleted() {
        completedCourses.getChildren().clear();


        for(String key : student.getCoursesDone().keySet()){
            if(student.getCoursesDone().get(key)){
                //Muutos maxCreditistä getTargetCreditsiin
                String credits = String.valueOf(allCourses.get(key).getTargetCredits());

                StringBuilder sb = new StringBuilder();
                sb.append(key + " " + credits + "op");

                String course = sb.toString();

                Text completedCourse = new Text(course);
                completedCourses.getChildren().add(completedCourse);
            }
        }
        allCredits.setText(String.format("%d" + "op", student.getCredits()));

    }

    /**
     * Saves current Student by using methods from SaveProgress.
     * Catches IOException if given file is not valid.
     */
    @FXML
    public void save() {
        System.out.println("Saving...");
        try {
            SaveProgress.saveStudent(student);

        } catch (IOException ex) {
            System.out.println("Couldn't save progress!");
        }
    }

    /**
     * Loads Student object from file, replaces the current Student.
     * Catches FileNotFoundException if given file doesn't exist.
     * Lastly refreshes the completed studies part of the UI view.
     */
    @FXML
    public void load() {
        System.out.println("Loading...");

        // Refresh-metodi?
        try {
            Controller.student = SaveProgress.loadStudent();
            studentNumber.setText(student.getNumber());
            studentName.setText(student.getName());
        } catch (FileNotFoundException e2) {
            System.out.println("File not found :(");
        }

        refreshStudiesCompleted();
    }

    /**
     * Switches the current degree shown in UI and sets it for the current Student.
     * NOTE: Switching degrees will reset the Students credits and courses done.
     * Lastly refreshes the completed studies part of the UI view.
     * 
     */
    @FXML
    public void switchDegree() {
        if (!searchBar.getEditor().getText().isEmpty() && degrees.containsKey(searchBar.getEditor().getText())) {
            Degree degree = degrees.get(searchBar.getEditor().getText());
            String s = degree.getName();
            student.setDegree(s);
            TreeItem<String> rootItem = GUITools.initializeTree(degree);
            mainView.setRoot(rootItem);
        }

        refreshStudiesCompleted();

    }

    /**
     * Refreshs the motivational image shown on the tab "motivaatio".
     * Image is AI generated by https://inspirobot.me/.
     */
    @FXML
    public void getNewMotivationalImage() {
        motivation.getEngine().load(GUITools.getMotivationalImageUrl());
    }

    /**
     * @param degrees HashMap containing String keys and Degree degrees to set.
     */

    public static void setDegrees(HashMap<String, Degree> degrees) {
        Controller.degrees = degrees;
    }

    /**
     * @param student Student-object to be set onto the Controller.
     */
    public static void setStudent(Student student) {
        Controller.student = student;
    }

    /**
     * @param degree Degree-object to be set onto the Controller.
     */
    public static void setDegree(Degree degree) {
        Controller.degree = degree;
    }

    /**
     * @param element String element to be set onto the Controller.
     */
    public static void setSelectedElement(String element) {
        Controller.selectedElement = element;
    }

    /**
     * @param c Course to be added to the allCourses HashMap
     */
    public static void addCourses(Course c) {
        allCourses.put(c.getName(), c);
    }

    /**
     * @param m DegreeModule to be added to the allModules HashMap.
     */
    public static void addModules(DegreeModule m) {
        allModules.put(m.getName(), m);
    }

    /**
     * Clears the allCourses and allModules HashMaps.
     */
    public static void clearMaps() {
        if (allCourses != null) {
            allCourses.clear();
        }
        if (allModules != null) {
            allModules.clear();
        }

    }

    /**
     * @param name A String containing the search key.
     * @return Course The Course object corresponding with the search key.
     */
    public static Course searchCourse(String name) {
        if (allCourses.containsKey(name)) {
            return allCourses.get(name);
        } else {
            return null;
        }
    }

    /**
     * @param name A String containing the search key.
     * @return DegreeModule The DegreeModule object corresponding with the search key.
     */
    public static DegreeModule searchModule(String name) {
        if (allModules.containsKey(name)) {
            return allModules.get(name);
        } else {
            return null;
        }
    }

    /**
     * Closes the program.
     */
    @FXML
    public void closeProgram() {
        Platform.exit();
        System.exit(0);
    }

    /**
     * TODO: Dokumentoi
     * 
     * @param item
     */
    private void addCreditsToTree(TreeItem<String> item) {
        if (item.getParent().getParent() != null) {
            addCreditsToTree(item.getParent());
            if (searchModule(item.getParent().getValue()) == null) {
                String value = item.getParent().getValue();
                StringBuilder sb = new StringBuilder();
                String[] splitValue = value.split(" ");
                int length = splitValue.length;
                String last = splitValue[length - 1];
                String pointsSplit = last.split("/")[0];
                String prevPoints = pointsSplit.substring(0, pointsSplit.length() - 2);
                int prevPointsNumb = Integer.parseInt(prevPoints);
                String[] name = Arrays.copyOf(splitValue, length - 1);
                for (String s : name) {
                    sb.append(s).append(" ");
                }
                sb.setLength(sb.length() - 1);
                if (courseCheckBox.isSelected()) {
                    Course c = searchCourse(Controller.selectedElement);
                    sb.append(" ").append(c.getTargetCredits() + prevPointsNumb).append("op/")
                            .append(last.split("/")[1]);
                    item.getParent().setValue(sb.toString());
                    
                    
                }
                else{
                    Course c = searchCourse(Controller.selectedElement);

                    DegreeModule m = searchModule(sb.toString());
                    sb.append(" ").append(prevPointsNumb - c.getTargetCredits()).append("op/")
                            .append(m.getTargetCredits()).append("op");
                    item.getParent().setValue(sb.toString());
                    
                }
            }
        }
    }
    private String[] splitString(String string){
        String[] splitValue = string.split(" ");
        int length = splitValue.length;
        String[] name = Arrays.copyOf(splitValue, length - 1);
        return name;
    }
    private String combineString(String[] nameArray){
        StringBuilder sb = new StringBuilder();
        for (String s : nameArray) {
            sb.append(s).append(" ");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

}
