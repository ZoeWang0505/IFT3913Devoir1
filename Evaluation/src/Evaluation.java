import org.json.JSONObject;
//import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

public class Evaluation {

    public static void findAllFilesInFolder(File folder, ArrayList<String> pathlist){
        try {
            
            for (File file : folder.listFiles()) {
                if (!file.isDirectory()) {
                    //
                    String fileName = file.getName();
                    if(fileName.endsWith(".java")){
                        pathlist.add(file.getPath());
                    }
                    //
                } else {
                    findAllFilesInFolder(file, pathlist);
                }
                
            }
        } catch (IllegalArgumentException iae) {
            System.out.println("File Not Found");
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

        File folder = null;
        folder = new File(folderPath);

        ArrayList<String> pathlist = new ArrayList<String>();
        findAllFilesInFolder(folder, pathlist);

    }
}