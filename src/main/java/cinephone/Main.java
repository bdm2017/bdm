package cinephone;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import com.dropbox.core.v2.files.*;
import com.dropbox.core.DbxException;
import java.io.*;
import java.util.ArrayList;
import cinephone.ImageComparaison;

public class Main {
	
	/* image file names and movie data arrays  */
	static ArrayList<String> imageTab;
	static ArrayList<String> moviesDataTab;
	
//  private static final String ACCESS_TOKEN = "lBzf_Shi9McAAAAAAAAD9LBIUIN9kvTuhlIQyavZf4ph_KtXrfrcVPcBNLJjjB-a";
  private static final String ACCESS_TOKEN = "T0IcR3FGlqIAAAAAAAAAW84BnKiFFEoJ3YIyMRUYPc3bomqfaAyDYBI2DgJnE5Ug";

    public static void main(String args[]) throws Exception {
    	
    	/**** Files names ****/
    	
    	String dataPath ="/home/pierre/workspace/TrouveAffiche/data";
        String imageFile = "image.jpg";
        String csvFile = "affiches.csv";
        String csvFileReturn = "dataMovie.csv";
        String stopFile = "stopServer.txt";
    	ImageComparaison imgComp = new ImageComparaison();
        
    	/**** BDD init ****/
    	
		/* Array Init */
		moviesDataTab = new ArrayList<String>();
		imageTab = new ArrayList<String>();	
				
		/* read database affiches.csv*/
		readCSVData(dataPath + "/" + csvFile, dataPath);
		/* pretreatment for KeyPOints in OpenCV*/
		System.out.println("OpenCV Pretreatment");
		ImageComparaison.init_database_keyPoints(imageTab, imgComp.featureDetector, imgComp.descriptorExtractor,imgComp.database_keyPoints, imgComp.database_descriptors);

		
		/**** Server : init****/    	
    	
        /* Create connexion */
        DbxRequestConfig config = new DbxRequestConfig("dropbox/java", "en_US");
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
        
        /* Get current account info */
        FullAccount account = client.users().getCurrentAccount();
        //System.out.println(account.getName().getDisplayName());
        
        
        /**** Server: Loop ****/
        
        boolean run = true;
        try 
        { 
        	/* clean stopfile on the server */
        	client.files().delete("/" + stopFile);
    	} 
        catch (DbxException exception)
		{
	    	System.out.println("stop server file has already been deleted");
		}
        
        System.out.println("Server ON");
        /* Loop */
        while (run){        	
        
	        /**** Server: Image Viewer ****/
	        
	        boolean Noimage = true;
	        System.out.println("Waiting image...");
	        while (Noimage)
	        {
	        	Thread.sleep(1000);
	        	Noimage = downloadImage(imageFile, dataPath, client);
	        }
	        System.out.println("Image received");
	        
	        
	        
	        /**** BDD : image analysis ****/
	        
	        /* read image array */
			for(int i = 0 ; i < imageTab.size(); i++){
			  String myImage = imageTab.get(i);
			  //System.out.println(myImage);
			}
			
			int indexFind = imgComp.comparaisonFinal(imageTab, dataPath + "/image.jpg");
			
			/* write in a scv reply file */	
			if (indexFind != -1){
					
				String validInfo = moviesDataTab.get(indexFind);
				writeCSVReply(dataPath + "/" + csvFileReturn, validInfo);
				
				
				/**** Serveur : Upload reply ****/       
				
				boolean noSending = true;
				noSending = uploadCSV(csvFileReturn, dataPath, client);
				if (!noSending) System.out.println("CSV file sent");
			
				/* read stop file if existed */
				run = downloadStopfile(stopFile, dataPath, client);
			} else System.out.println("Affiche non trouvée");
		}
		System.out.println("Server OFF");
    }
    
    /* DATABASE METHODS */
    
    /* readCSVData - read a CSV file and save data in arrays*/
	static public void readCSVData(String csvFile, String imagePath)
	{	
        String line = "";
        String cvsSplitBy = "\t";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), "UTF8"))) {

            while ((line = br.readLine()) != null) {
                String[] dataMovie = line.split(cvsSplitBy);
                //System.out.println("Nom: " + dataMovie[0] + "\nDate de sortie: " + dataMovie[1] + "\nRealisateur: " + dataMovie[2] + "\nDescription: " + dataMovie[3] + "\nLien de la BA: " + dataMovie[4] + "\nLiens sceances: " + dataMovie[5] + "\nNom fichier image: " + dataMovie[6] + "\n");
				moviesDataTab.add(dataMovie[0] + "\t" + dataMovie[1] + "\t" + dataMovie[2] + "\t" + dataMovie[3] + "\t" + dataMovie[4] + "\t" + dataMovie[5]);
				imageTab.add(imagePath +"/images/" +dataMovie[6]);
			}
        } catch (IOException e) {
            e.printStackTrace();
        }	
	}
	
	/* writeCSVReply - write a data line in a CSV file*/
	static public void writeCSVReply (String csvFile, String validInfo)
	{
		File f = new File(csvFile);
		
		String columnName = "Nom: \tDate de sortie: \tRéalisateur: \tDescription: \tBande d'annonce: \tSéances: \n";
		validInfo = columnName + validInfo;
		
		try
		{
			FileWriter fw = new FileWriter (f);
			fw.write (validInfo);
			fw.write ("\r\n");
			fw.close();
		}
		catch (IOException exception)
		{
			System.out.println ("error when reading CSV file : " + exception.getMessage());
		}
	}
    
    /* SERVER METHODS */
    
	/* downloadImage - receive a server image and return false when the image exist, true otherwise */
    static public boolean downloadImage(String imageFile, String dataPath, DbxClientV2 client) throws Exception
	{      
    	try (OutputStream fos = new FileOutputStream(new File(dataPath + "/" + imageFile)))
    	{
            FileMetadata metadata = client.files().downloadBuilder("/Photos/" + imageFile).download(fos);
            fos.close();
            client.files().delete("/Photos/" + imageFile);       
    	}
	    catch (DbxException exception)
		{
			//System.out.println(imageFile + " does not exist");
			return true;
		}
    	
    	return false;
	}
    
    /* uploadCSV - send a CSV file to the server, return false if the sending success, true otherwise */
    static public boolean uploadCSV(String csvFile, String dataPath, DbxClientV2 client) throws Exception
	{
    	try 
    	{ 
    		client.files().delete("/Photos/" + csvFile);
    	} catch (DbxException exception)
		{
	    	System.out.println(csvFile + "file has been recovered by the client");
		}
        try (InputStream in = new FileInputStream(dataPath + "/" + csvFile)) {
            FileMetadata metadata = client.files().uploadBuilder("/Photos/" + csvFile).uploadAndFinish(in);
            in.close();
        } catch (DbxException exception)
		{
	    	System.out.println(csvFile + " file still exist");
	    	return true;
		}
        return false;
	}
    
    /* readServerFiles - read all files in the server */
    static public void readServerFiles(DbxClientV2 client) throws Exception
	{
        ListFolderResult result = client.files().listFolder("");
        while (true) {
            for (Metadata metadata : result.getEntries()) {
                System.out.println(metadata.getPathLower());
            }
            if (!result.getHasMore()) {
                break;
            }
            result = client.files().listFolderContinue(result.getCursor());        
        }
	}
    
	/* downloadStopfile - receive a stop file and return false when the file exist, true otherwise */
    static public boolean downloadStopfile(String stopFile, String dataPath, DbxClientV2 client) throws Exception
	{      
    	try (OutputStream fos = new FileOutputStream(new File(dataPath + "/" + stopFile)))
    	{
            FileMetadata metadata = client.files().downloadBuilder("/" + stopFile).download(fos);
            fos.close();
            client.files().delete("/" + stopFile);
    	}
	    catch (DbxException exception)
		{
			//System.out.println("Stop file does not exist");
			return true;
		}
    	
    	return false;
	}
}