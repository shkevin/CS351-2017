package sample;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import static java.lang.Math.random;

public class Main extends Application
{

  final Text actiontarget = new Text();
  final Text hint = new Text("Username: admin, Password: admin");

  @Override
  public void start(Stage primaryStage) throws Exception
  {
    Group circles = new Group();
    Group root = new Group();
    Timeline timeline = new Timeline();
    addCircles(root, circles);
    addAnimation(timeline, circles);
    Scene scene2 = new Scene(root, 500, 500, Color.BLACK);
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(25, 25, 25, 25));

    Text scenetitle = new Text("Welcome");
    scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
    grid.add(scenetitle, 0, 0, 2, 1);

    Label userName = new Label("User Name:");
    grid.add(userName, 0, 1);

    TextField userTextField = new TextField();
    grid.add(userTextField, 1, 1);

    Label pw = new Label("Password:");
    grid.add(pw, 0, 2);

    PasswordField pwBox = new PasswordField();
    grid.add(pwBox, 1, 2);

    Button btn = new Button("Sign in");
    HBox hbBtn = new HBox(8);
    hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
    hbBtn.getChildren().add(btn);
    grid.add(hbBtn, 1, 4);

    grid.add(actiontarget, 1, 6);
    grid.add(hint, 1, 7);

    btn.setOnAction(event ->
    {
      buttonClicked(actiontarget, event, userTextField.getText(), pwBox.getText());
      if (actiontarget.getText().equals("signing in"))
      {
        primaryStage.setScene(scene2);
        timeline.play();
      }
    });

    Scene scene1 = new Scene(grid, 325, 275);
    primaryStage.setScene(scene1);
    primaryStage.show();
  }

  private void addAnimation(Timeline timeline, Group circles)
  {
    for (Node circle: circles.getChildren()) {
      timeline.getKeyFrames().addAll(
              new KeyFrame(Duration.ZERO, // set start position at 0
                      new KeyValue(circle.translateXProperty(), random() * 800),
                      new KeyValue(circle.translateYProperty(), random() * 600)
              ),
              new KeyFrame(new Duration(40000), // set end position at 40s
                      new KeyValue(circle.translateXProperty(), random() * 800),
                      new KeyValue(circle.translateYProperty(), random() * 600)
              )
      );
    }
  }

  private void addCircles(Group root, Group circles)
  {
    for (int i = 0; i < 30; i++)
    {
      Circle circle = new Circle(150, Color.web("white", .05));
      circle.setStrokeType(StrokeType.OUTSIDE);
      circle.setStroke(Color.web("white", 0.16));
      circle.setStrokeWidth(4);
      circles.getChildren().add(circle);
    }
    root.getChildren().add(circles);
  }

  private void buttonClicked(Text actiontarget, ActionEvent e, String user, String password)
  {
    actiontarget.setFill(Color.FIREBRICK);
    Label signIn;
    signIn = checkAction(user, password);
    actiontarget.setText(signIn.getText());
  }

  private Label checkAction(String user, String password)
  {
    Label label = new Label();
    if (user.equals("admin") && password.equals("admin")) label.setText("signing in");
    else label.setText("sign in failed");
    return label;
  }


  public static void main(String[] args)
  {
    launch(args);
  }
}
