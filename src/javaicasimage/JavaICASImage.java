/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaicasimage;


import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Base64;
import javafx.embed.swing.SwingFXUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javax.imageio.ImageIO;


import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.TIFFDecodeParam;
import javax.media.jai.JAI;
import java.io.ByteArrayOutputStream;

import java.awt.image.renderable.ParameterBlock;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;

import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javax.media.jai.RenderedOp;



/**
 *
 * @author krissada.r
 */
public class JavaICASImage extends Application {
    
    private static byte[] decodedGF;
    private static byte[] decodedBWF;
    private static byte[] decodedBWB;
    private static ImageView imvGF = new ImageView();
    private static ImageView imvBWF = new ImageView();
    private static ImageView imvBWB = new ImageView();
    private static GridPane gridpane = new GridPane();
    private TextField txtTable = new TextField();
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.setTitle("View Cheque Image");
        Group root = new Group();
        Scene scene = new Scene(root, 900, 600, Color.WHITE);
        
        
        gridpane.setPadding(new Insets(5));
        gridpane.setHgap(10);
        gridpane.setVgap(10);
        
        
        
        //final ImageView imvBWF = new ImageView();
        //final ImageView imvBWB = new ImageView();
        
        Label lblServer = new Label("Server:");
        lblServer.setStyle("-fx-font-weight: bold");
        Label LblUsername = new Label("Username:");
        LblUsername.setStyle("-fx-font-weight: bold");
        Label LblPassword = new Label("Password:");
        LblPassword.setStyle("-fx-font-weight: bold");
        
        TextField txtServer = new TextField();
        TextField txtUserName = new TextField();
        TextField txtPassword = new TextField();
        
        HBox hbServerCon = new HBox(lblServer,txtServer,LblUsername,txtUserName,LblPassword,txtPassword);
        hbServerCon.setSpacing(10.0);
        //hbServerCon.setPadding(new Insets(5,0,0,0));
        hbServerCon.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTable = new Label("Table:");
        lblTable.setStyle("-fx-font-weight: bold");
        txtTable.setPrefWidth(500);
        
        
        Label lblUIC = new Label("UIC:");
        lblUIC.setStyle("-fx-font-weight: bold");
       
        TextField txtUIC = new TextField();
        txtUIC.setPrefWidth(200);
        
        Button getImage = new Button("Submit ...");
        
        HBox hboxTable = new HBox(lblTable,txtTable);
        hboxTable.setSpacing(10.0);
        //hboxTable.setPadding(new Insets(0,0,0,0));
        hboxTable.setAlignment(Pos.CENTER_LEFT);
        
        HBox hboxUIC = new HBox(lblUIC,txtUIC,getImage);
        hboxUIC.setSpacing(10.0);
       // hboxUIC.setPadding(new Insets(5,0,0,0));
        hboxUIC.setAlignment(Pos.CENTER_LEFT);
        
        txtUIC.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode() == KeyCode.ENTER)  {
                    //String text = txtUIC.getText();
                    displayImage(txtUIC.getText(),txtServer.getText(),txtUserName.getText(),txtPassword.getText(),txtTable.getText());

                }
            }
        });
        
        
        getImage.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent arg0) {
                //"2017090600481101000033"
                displayImage(txtUIC.getText(),txtServer.getText(),txtUserName.getText(),txtPassword.getText(),txtTable.getText());
                
                
            }
        });
        
        StackPane zoomPane = new StackPane();
        zoomPane.getChildren().add(new Circle(100, 100, 10));
        zoomPane.getChildren().add(new Circle(200, 200, 20));

        // Create operator
        AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator();

        // Listen to scroll events (similarly you could listen to a button click, slider, ...)
        zoomPane.setOnScroll(new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                double zoomFactor = 1.5;
                if (event.getDeltaY() <= 0) {
                    // zoom out
                    zoomFactor = 1 / zoomFactor;
                }
                zoomOperator.zoom(zoomPane, zoomFactor, event.getSceneX(), event.getSceneY());
            }
        });


        
        
        
        gridpane.add(hbServerCon,1,1);
        gridpane.add(hboxTable,1,2);
        gridpane.add(hboxUIC, 1,3);
        
        root.getChildren().add(gridpane);
        root.getChildren().add(zoomPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public class AnimatedZoomOperator {

    private Timeline timeline;

    public AnimatedZoomOperator() {         
         this.timeline = new Timeline(60);
    }

    public void zoom(Node node, double factor, double x, double y) {    
        // determine scale
        double oldScale = node.getScaleX();
        double scale = oldScale * factor;
        double f = (scale / oldScale) - 1;

        // determine offset that we will have to move the node
        Bounds bounds = node.localToScene(node.getBoundsInLocal());
        double dx = (x - (bounds.getWidth() / 2 + bounds.getMinX()));
        double dy = (y - (bounds.getHeight() / 2 + bounds.getMinY()));

        // timeline that scales and moves the node
        timeline.getKeyFrames().clear();
        timeline.getKeyFrames().addAll(
            new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), node.getTranslateX() - f * dx)),
            new KeyFrame(Duration.millis(200), new KeyValue(node.translateYProperty(), node.getTranslateY() - f * dy)),
            new KeyFrame(Duration.millis(200), new KeyValue(node.scaleXProperty(), scale)),
            new KeyFrame(Duration.millis(200), new KeyValue(node.scaleYProperty(), scale))
        );
        timeline.play();
    }
}
    
    
    public static byte[] TiffToJpg(String face) {
    //File tiffFile = new File(tiff);
        
        //ByteArrayInputStream bisBWF = new ByteArrayInputStream(decodedBWF);
        //ByteArraySeekableStream s=null;// = new ByteArraySeekableStream();
        BufferedImage bufferedImage=null;
        TIFFDecodeParam decodeParam = new TIFFDecodeParam();
        ByteArraySeekableStream stream=null;// = new ByteArraySeekableStream(filepath + filename);
        
        if ("BWF".equals(face)) {
            try {
            stream = new ByteArraySeekableStream(decodedBWF);
            decodeParam.setDecodePaletteAsShorts(true);
            ParameterBlock params = new ParameterBlock();
            params.add(stream);
            RenderedOp image1 = JAI.create("tiff", params);
            bufferedImage=image1.getAsBufferedImage();
            } catch (IOException ex) {
                    Logger.getLogger(JavaICASImage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            
            try {
            stream = new ByteArraySeekableStream(decodedBWB);
            decodeParam.setDecodePaletteAsShorts(true);
            ParameterBlock params = new ParameterBlock();
            params.add(stream);
            RenderedOp image1 = JAI.create("tiff", params);
            bufferedImage=image1.getAsBufferedImage();
            } catch (IOException ex) {
                    Logger.getLogger(JavaICASImage.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        try {
            
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write( bufferedImage, "jpg", baos );
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            
            
            return imageInByte;
            
            //FileSeekableStream obj_FileSeekableStream = new FileSeekableStream(new File(str_TiffUrl));
      //ImageDecoder obj_ImageDecoder = ImageCodec.createImageDecoder("tiff", s, null);
      //RenderedImage obj_RenderedImage = obj_ImageDecoder.decodeAsRenderedImage();
      //JAI.create("filestore",obj_RenderedImage,str_JpgFileDestinationUrl, "jpeg");
      //obj_RenderedImage = null;
      //obj_ImageDecoder = null;
      //obj_FileSeekableStream.close();
        } catch (IOException ex) {
            Logger.getLogger(JavaICASImage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
  }
    
    
    private static void displayImage(String UIC,String Server,String User,String Password,String Table) {
        
                database(UIC,Server,User,Password,Table);
                
                ByteArrayInputStream bisGF = new ByteArrayInputStream(decodedGF);
                ByteArrayInputStream bisBWF = new ByteArrayInputStream(TiffToJpg("BWF"));
                ByteArrayInputStream bisBWB = new ByteArrayInputStream(TiffToJpg("BWB"));
                
                
                BufferedImage buffGF=null;
                BufferedImage buffBWF=null;
                BufferedImage buffBWB=null;
                
                try {
                    buffGF = ImageIO.read(bisGF);
                    buffBWF = ImageIO.read(bisBWF);
                    buffBWB = ImageIO.read(bisBWB);
                } catch (IOException ex) {
                    Logger.getLogger(JavaICASImage.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                
                Image imageGF = SwingFXUtils.toFXImage(buffGF, null);
                imvGF.setImage(imageGF);
                imvGF.setFitHeight(200);
                imvGF.setFitWidth(400);
                
                Image imageBWF = SwingFXUtils.toFXImage(buffBWF, null);
                imvBWF.setImage(imageBWF);
                imvBWF.setFitHeight(200);
                imvBWF.setFitWidth(400);
                
                Image imageBWB = SwingFXUtils.toFXImage(buffBWB, null);
                imvBWB.setImage(imageBWB);
                imvBWB.setFitHeight(200);
                imvBWB.setFitWidth(400);
                
        
                //Image imageBWF=null;
                //Image imageBWB=null;
        
                /*
                if (buffBWF !=null) {
                    imageBWF = SwingFXUtils.toFXImage(buffBWF, null);
                }


                if (buffBWB !=null) {
                    imageBWB = SwingFXUtils.toFXImage(buffBWB, null);
                }
                */
                
                //imvGF.setImage(imageGF);
                //imvBWF.setImage(imageBWF);
                //imvBWB.setImage(imageBWB);

                
                
               
                VBox pictureRegionBW = new VBox(imvBWF,imvBWB);
                pictureRegionBW.setSpacing(5.0);
                pictureRegionBW.setPadding(new Insets(0,5,5,5));
                
                HBox pictureRegion = new HBox(imvGF,pictureRegionBW);
                pictureRegion.setSpacing(5.0);
                pictureRegion.setPadding(new Insets(0,5,5,5));
                
        
                gridpane.add(pictureRegion, 1,4);
    }
    
    
    public static void database(String UIC,String Server,String User,String Password,String table) {
        Connection connect = null;
        String connString=null;
                
                
		
		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                        connString = "jdbc:sqlserver://"+Server+";DatabaseName=master;user="+User+";Password="+Password;
			connect = DriverManager.getConnection(connString);
                        //connect =  DriverManager.getConnection("jdbc:sqlserver://172.30.132.75:1433;DatabaseName=TFB_Image;user=su;Password=ncr");
			if(connect != null){
				//System.out.println("Database Connected.");
                                Statement statement = connect.createStatement();
                                
                                
                                String queryString = "select IMAGE_FILE1,IMAGE_FILE2,IMAGE_FILE3 from "+table+" with(readuncommitted) where UIC = '"+UIC+"'";
                          
                                ResultSet rs = statement.executeQuery(queryString);
                                while (rs.next()) {
                                    decodedGF = Base64.getDecoder().decode(rs.getString(rs.findColumn("IMAGE_FILE1")));
                                    decodedBWF = Base64.getDecoder().decode(rs.getString(rs.findColumn("IMAGE_FILE2")));
                                    decodedBWB = Base64.getDecoder().decode(rs.getString(rs.findColumn("IMAGE_FILE3")));
                                    
                                    //byte[] decodedBytes = decoder.decodeBuffer(imgaeEncode);
                                    
                                    //System.out.println("-- decode --\n"+ new String(decodedBWF, "utf-8") );
                                    
                                    //return new String(decoded, "utf-8");
                                   
                                    
                                }
			} else {
				System.out.println("Database Connect Failed.");
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			connect.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Application.launch(args);
    }
    
}
