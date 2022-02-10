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
   static String cHeaderlist [] = {"chemin", "class", "classe_LOC", "classe_CLOC", "classe_DC", "WMC", "classe_BC"};
   static String pHeaderlist [] = {"chemin", "paquet", "paquet_LOC", "paquet_CLOC", "paquet_DC","WCP", "paquet_BC"};
   static enum commentType {SINGLE_LINE, MULTICOMM_BEGIN, NON_COMM, MULTICOMM_END, IN_MULTCOMM};

   static enum inLineType{ FUNC_BEGIN, IN_FUNC, FUNC_END, NON_FUNC};
   
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
     * getInLineType get InlineType 
     * @param line
     * @param inFunction
     * @return inLineType from enum inLineType
     */
    static inLineType getInLineType(String line, boolean inFunction){
        
        if(inFunction){
            if(line.trim().compareTo("}") == 0)
                return inLineType.FUNC_END;
            else
                return inLineType.IN_FUNC;
        }
        else{
            //Match a java function title
            String pattern = "^\\s*(static\\s)*(public|private|protected)\\s.*\\(.*\\)\\s*\\{";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(line);
            if(m.find()){
                return inLineType.FUNC_BEGIN;
            }
            return inLineType.NON_FUNC;
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

            //signal for if a function started.
            boolean inFunction = false;
            
            //nodes number in a Method
            int nodeNum = 0;
            int WMC = 0;

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

                if(comType == commentType.MULTICOMM_BEGIN || 
                comType == commentType.IN_MULTCOMM ||
                comType == commentType.MULTICOMM_END){
                    continue;
                }

                inLineType inline = getInLineType(line, inFunction);
                switch (inline){
                    case FUNC_BEGIN:
                        inFunction = true;
                        nodeNum = 0;
                        break;
                    case FUNC_END:
                        WMC += nodeNum +1; //by form: WMC = 1 + noteNum  
                        nodeNum = 0;
                        inFunction = false;       
                        break; 
                    case IN_FUNC:
                        if(comType != commentType.IN_MULTCOMM){// if it's not in comment blocks
                            if(isNodeInFunction(line, inline)){
                                nodeNum ++;
                            }
                        }
                        break; 
                    case NON_FUNC:
                        break; 
                    default:
                        break;
                }

            }
            eva_class.put(cHeaderlist[2], lineNumber);//classe_LOC
            eva_class.put(cHeaderlist[3], commtaireline); //classe_CLOC    
            eva_class.put(cHeaderlist[4], (double)commtaireline/lineNumber); //classe_DC   
            eva_class.put(cHeaderlist[5], WMC); //WMC  
            eva_class.put(cHeaderlist[6], (double)commtaireline/lineNumber/ WMC); //classe_BC  
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
        
       int paquet_LOC =  (int) eva_paquet.get(pHeaderlist[2])+ (int) eva_class.get(cHeaderlist[2]);
       eva_paquet.put(pHeaderlist[2], paquet_LOC);

       int paquet_CLOC =  (int) eva_paquet.get(pHeaderlist[3]) + (int) eva_class.get(cHeaderlist[3]);
       eva_paquet.put(pHeaderlist[3], paquet_CLOC);

       eva_paquet.put(pHeaderlist[4], (double)paquet_CLOC / paquet_LOC);
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
     * isNodeInFunction: get if node in the line
     * @param line
     * @param inline
     * @return
     */
    static boolean isNodeInFunction(String line, inLineType inline){
        //Match a key word of node
        String pattern_node = "^(\\s)*(?!\\/\\/)(if|while|case)";
        Pattern r = Pattern.compile(pattern_node);
        Matcher m = r.matcher(line);
        if(m.find()){
            return true;
        }
        return false;
    }

    /**
     * This function is for evaluating the target folder and prepare for the code quality report 
     * @param folder the folder we a going to evaluate
     * @param path the path for building the infomation fo file path
     * @param classdata the data of JSONArray which used for generalize the final class csv report 
     * @param paquetdatathe data of JSONArray which used for generalize the final package csv report 
     */
    static void evaluate(File folder, String path,JSONArray classdata , JSONArray paquetdata){
        try {
            String packageName ="";
            JSONObject eva_paquet = null;
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

                        if(packageName == "")
                            packageName = getPackageName(file);

                        if(eva_paquet == null){
                            eva_paquet = new JSONObject();
                            eva_paquet.put(pHeaderlist[0], path);
                            eva_paquet.put(pHeaderlist[1], packageName);   
                            eva_paquet.put(pHeaderlist[2], 0); 
                            eva_paquet.put(pHeaderlist[3], 0); 
                            eva_paquet.put(pHeaderlist[4], 0); 
                            paquetdata.put(eva_paquet);
                        }
                        parsingPaquet(eva_paquet, eva_class);
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
        //writeCSV(paquetData, csvPaquePath, pHeaderlist);
    }
}