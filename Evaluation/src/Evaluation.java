import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.apache.commons.io.FileUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;

public class Evaluation {
   static String cHeaderlist [] = {"chemin", "class", "classe_LOC", "classe_CLOC", "classe_DC"};
   static String pHeaderlist [] = {"chemin", "paquet", "paquet_LOC", "paquet_CLOC", "paquet_DC"};
   static enum commentType {SINGLE_LINE, MULTICOMM_BEGIN, NON_COMM, MULTICOMM_END, IN_MULTCOMM};
   
    /**
     * function for check if the line of string contains any comments. 
     * @param line one line of string from the java file
     * @param inMultiCommtaire if this line is in a multi comment block
     * @return commentType defined the comment type that the input string has
     */
    static commentType containCommtaire(String line, Boolean inMultiCommtaire){
        if(!inMultiCommtaire){
            //Match if in single line comments such as \\ and /** */
            String pattern = "\\/\\*(\\*(?!\\/)|[^*])*\\*\\/|\\/\\/";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(line);
            if(m.find()){
                return commentType.SINGLE_LINE;
            } else {
                //Match if starting with /*
                pattern = "\\/\\*";
                r = Pattern.compile(pattern);
                m = r.matcher(line); 
                if(m.find()){
                    return commentType.MULTICOMM_BEGIN;
                }
            }
            return commentType.NON_COMM;
        } else{
            String pattern = "(\\*(?!\\/)|[^*])*\\*\\/";//Match if end with */
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(line);
            if(m.find()){
                return commentType.MULTICOMM_END;
            }
            return commentType.IN_MULTCOMM;
        }

    }

    /**
     * Function for parsing java file line to line counting code line 
     * number and comment line number
     * @param file input java file
     * @param eva_class the JSONObject contains the code infomation from java file
     */
    static void parsingClass(File file,JSONObject eva_class){
        //Creating Scanner instance to read File in Java
        try {
            Scanner scan = new Scanner(file);

            //Reading each line of the file using Scanner class
            int lineNumber = 0;
            int commtaireline = 0;
            boolean inMultiCommtaire = false;
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                if(line.compareTo("") == 0)
                   continue;
                
                lineNumber++;
                commentType comType = containCommtaire(line, inMultiCommtaire);
                switch (comType){
                    case SINGLE_LINE:
                        commtaireline ++;
                        break;
                    case MULTICOMM_BEGIN:
                        commtaireline ++;
                        inMultiCommtaire = true;
                        break; 
                    case MULTICOMM_END:
                        inMultiCommtaire = false;
                        commtaireline ++;
                        break; 
                    case IN_MULTCOMM:
                        commtaireline ++;
                        break; 
                    default:
                        break;
                }
            }
            eva_class.put(cHeaderlist[2], lineNumber);//classe_LOC
            eva_class.put(cHeaderlist[3], commtaireline); //classe_CLOC    
            eva_class.put(cHeaderlist[4], (double)commtaireline/lineNumber); //classe_DC   
            scan.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Collect Infomation from all the java files of this package
     * @param eva_paquet JSONObject which contains le package infomation 
     * @param eva_class JSONObject which contains le class infomation 
     */
    static void parsingPaquet(JSONObject eva_paquet, JSONObject eva_class){
        
       int paquet_LOC;
       int paquet_CLOC;
        //TODO:
        //Get infomation from eva_class then calculate for updating package data


       //paquet_LOC : nombre de lignes de code d’un paquet (java package) -- la somme des LOC de ses classes
       //paquet_CLOC : nombre de lignes de code d’un paquet qui contiennent des commentaires
       //paquet_DC : densité de commentaires pour une classe : classe_DC = classe_CLOC / classe_LOC
       //paquet_DC : densité de commentaires pour un paquet : paquet_DC = paquet_CLOC / paquet_LOC
       //

    }

    /**
     * Get the package name the java file belongs to
     * @param file java file for evaluation
     * @return packge name of this file
     */
    static String getPackageName(File file){
        //Creating Scanner instance to read File in Java
        try {
            Scanner scan = new Scanner(file);

            //Reading each line of the file using Scanner class
            while(scan.hasNextLine()){
                String line = scan.nextLine();
                if(line.compareTo("") == 0)
                   continue;

                if(line.contains("package")){
                    int indexStart = line.indexOf("package") + 7;
                    String packageName = line.substring(indexStart, line.length() - 1).trim();
                    scan.close();
                    return packageName;
                }
            }
            scan.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * This function get JSONObject by packageName if it is existed in data
     * @param paquetdata paquet data is a JSONArray with all the package infomations
     * @param packageName check the package name if existed in paquetdata already
     * @return the JSONObject if the package existed. or null if it doesn't existed.
     */
    static JSONObject getPackageJSON(JSONArray paquetdata, String packageName){
        //TODO:
        for(int i = 0; i < paquetdata.length(); i++){
            JSONObject packageJSON = paquetdata.getJSONObject(i);
            if(packageJSON.getString("name").equals(packageName)){
                return packageJSON;
            }
        }
        //package not found
        return null;
    }

    /**
     * This function is for evaluating the target folder and prepare for the code quality report 
     * @param folder the folder we a going to evaluate
     * @param path the path for building the infomation fo file path
     * @param classdata the data of JSONArray which used for generalize the final class csv report 
     * @param paquetdata data of JSONArray which used for generalize the final package csv report
     */
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

                        String packageName = getPackageName(file);
                        JSONObject eva_paquet = getPackageJSON(paquetdata, packageName);
                        if(eva_paquet == null){
                            eva_paquet = new JSONObject();
                            eva_paquet.put(pHeaderlist[0], path);
                            eva_paquet.put(pHeaderlist[1], packageName);
                            parsingPaquet(eva_paquet, eva_class);
                            paquetdata.put(eva_paquet);
                        } else {
                            parsingPaquet(eva_paquet, eva_class);
                        }
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

    /**
     * Write the JSONArray data into csv file
     * @param jsonData JSONArray with all the infomation from java files.
     * @param filepath output file path for csv
     * @param headerlist column header strings for the csv file.
     */
    static void writeCSV(JSONArray jsonData, String filepath,String [] headerlist){
        try {

            File file=new File(filepath);
            String csv = "";
            for(int i = 0; i < headerlist.length; i ++){ 
                csv += headerlist[i] + ",";
            }
            csv += "\n";
            Iterator<Object> iterator = jsonData.iterator();
            while(iterator.hasNext()) {
                JSONObject obj = (JSONObject)iterator.next();
                for(int i = 0; i < headerlist.length; i ++){ 
                    String value = obj.get(headerlist[i]).toString();
                    csv += value + ",";
                }
                csv += "\n";
            }
            
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

        writeCSV(classData, csvClassPath, cHeaderlist);
        writeCSV(paquetData, csvPaquePath, pHeaderlist);
    }
}