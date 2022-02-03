import org.json.CDL;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.apache.commons.io.FileUtils;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
//import java.util.ArrayList;
import java.util.Scanner;

public class Evaluation {
   static String cHeaderlist [] = {"chemin", "class", "classe_LOC", "classe_CLOC", "classe_DC"};
   String pHeaderlist [] = {"chemin", "paquet", "paquet_LOC", "paquet_CLOC", "paquet_DC"};
   
    static Boolean containCommtaire(String line){
        //TODO:

        return false;
    }

    static void parsingClass(File file,JSONObject eva_class){
        //Creating Scanner instance to read File in Java
        try {
            Scanner scan = new Scanner(file);

            //Reading each line of the file using Scanner class
            int lineNumber = 0;
            int commtaireline = 0;
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                if(containCommtaire(line)){
                    commtaireline ++;
                }
                lineNumber++;
            }
            eva_class.put(cHeaderlist[3], lineNumber);//classe_LOC
            eva_class.put(cHeaderlist[4], commtaireline); //classe_CLOC    
             
            scan.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    static void parsingPaquet(File file,JSONObject eva_class){
 //TODO:
    }


    static void evaluate(File folder, String path,JSONArray classdata , JSONArray paquetdata){
        try {
             
            for (File file : folder.listFiles()) {
                if (!file.isDirectory()) {
                    //
                    String fileName = file.getName();
                    if(fileName.endsWith(".java")){
                        JSONObject eva_class = new JSONObject();
                        String className = file.getName();
                        String chemin = path + "/" + className;

                        
                        eva_class.put(cHeaderlist[0], chemin); //chemin
                        eva_class.put(cHeaderlist[1], className.substring(0, className.indexOf(".java")));//class
                        parsingClass(file, eva_class);
                        classdata.put(eva_class);
                    }
                    //
                } else {
                    String newPath = path + "/" + file.getName();
                    evaluate(file, newPath, classdata, paquetdata);
                }
                
            }
        } catch (IllegalArgumentException iae) {
            System.out.println("File Not Found");
        }
    }

    static void writeCSV(JSONArray jsonData, String filepath){
        try {

            File file=new File(filepath);
            String csv = CDL.toString(jsonData);
            FileUtils.writeStringToFile(file, csv, "UTF-8");

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }        
    }
    

    public static void main(String[] args){
  
        String resourceName = "config.json";

        InputStream inputStream = Evaluation.class.getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new NullPointerException("Cannot find resource file " + resourceName);
        }

        JSONTokener tokener = new JSONTokener(inputStream);
        JSONObject jsonObj = new JSONObject(tokener);
        String folderPath = jsonObj.getString("TEST_PROJECT_PATH");
        String csvClassPath = jsonObj.getString("CSV_CLASS_PATH");
        String csvPaquePath = jsonObj.getString("CSV_PAQUET_PATH");

        File folder = null;
        folder = new File(folderPath);

        String path = "";

        JSONArray classData = new JSONArray();
        JSONArray paquetData = new JSONArray();
        evaluate(folder, path, classData, paquetData);

        writeCSV(classData, csvClassPath);
        writeCSV(paquetData, csvPaquePath);
    }
}