package cinephone;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
//import pour test de MSSIM.
//+highgui + CVType
//TEST SURF DETECTOR


class ImageComparaison {

	public List<MatOfKeyPoint> database_keyPoints;
	public List<MatOfKeyPoint> database_descriptors;
	public FeatureDetector featureDetector;
	public DescriptorExtractor descriptorExtractor;
	
	ImageComparaison(){
		database_keyPoints = new LinkedList<MatOfKeyPoint>();
		database_descriptors = new LinkedList<MatOfKeyPoint>();
		featureDetector = FeatureDetector.create(FeatureDetector.SURF);
		descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
		
	}
	
	
  static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
  
  public static void init_keyPoints_descriptors(Mat image, MatOfKeyPoint keyPoints, MatOfKeyPoint descriptors, FeatureDetector featureDetector, DescriptorExtractor descriptorExtractor){

	  if (keyPoints == null)
		  keyPoints = new MatOfKeyPoint();	  
	  if (descriptors == null)
		  descriptors = new MatOfKeyPoint();

      featureDetector.detect(image, keyPoints);
      
      descriptorExtractor.compute(image, keyPoints, descriptors);
  }

  public static int check_similarity_surf_2(List<MatOfDMatch> matches, double d){
	   int nbmatch = 0;
	   for (int i = 0; i < matches.size(); i++) {
		   MatOfDMatch matofDMatch = matches.get(i);
		   DMatch[] dmatcharray = matofDMatch.toArray();
		   DMatch m1 = dmatcharray[0];
	       DMatch m2 = dmatcharray[1];
	       if (m1.distance <= m2.distance * d) {
	    	   nbmatch++;
	       }
	   }
	  return nbmatch;
  }
  
  public static int check_similarity_surf(Mat user_poster, Mat real_poster, double d){
      
	  int nbmatch=0;
	  

      MatOfKeyPoint userKeyPoints = new MatOfKeyPoint(); 
      MatOfKeyPoint userDescriptors = new MatOfKeyPoint();
      MatOfKeyPoint realPosterKeyPoints = new MatOfKeyPoint();
      MatOfKeyPoint realPosterDescriptors = new MatOfKeyPoint();

      FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
      DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
            
      init_keyPoints_descriptors(user_poster, userKeyPoints, userDescriptors, featureDetector, descriptorExtractor);
      init_keyPoints_descriptors(real_poster, realPosterKeyPoints, realPosterDescriptors, featureDetector, descriptorExtractor);
      
      List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
      DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
      descriptorMatcher.knnMatch(userDescriptors, realPosterDescriptors, matches, 2);
      
      nbmatch = check_similarity_surf_2(matches, d);
      
      return nbmatch;
}
 
  
public static void init_database_keyPoints(ArrayList<String> listDatabase, FeatureDetector featureDetector, DescriptorExtractor descriptorExtractor,List<MatOfKeyPoint> database_keyPoints, List<MatOfKeyPoint> database_descriptors){
	Mat poster =  Mat.ones(new org.opencv.core.Size(110, 110), CvType.CV_32F);
    MatOfKeyPoint realPosterKeyPoints = null;
    MatOfKeyPoint realPosterDescriptors = null;
	
	for (int i=0; i < listDatabase.size(); i++){
		realPosterKeyPoints = new MatOfKeyPoint();
		realPosterDescriptors = new MatOfKeyPoint();
		poster = Highgui.imread(listDatabase.get(i), CvType.CV_32F);
		init_keyPoints_descriptors(poster, realPosterKeyPoints, realPosterDescriptors, featureDetector, descriptorExtractor);
		database_keyPoints.add(realPosterKeyPoints);
		database_descriptors.add(realPosterDescriptors);
	}
}
 

public static int correspond_film_v2 (String image_test, List<MatOfKeyPoint> database_keyPoint, List<MatOfKeyPoint> database_descriptors, FeatureDetector featureDetector,  DescriptorExtractor descriptorExtractor){
	  double max_sum = 0.0, current =0.0;
	  int w,h,indice=-1;
	
	  //load and resize image for speed (cropping)
	  Mat user_poster = Highgui.imread(image_test, CvType.CV_32F);
	  w = (int)user_poster.size().width;
	  h = (int)user_poster.size().height;
	  System.out.println("ASize= "+user_poster.height() + "," + user_poster.width());
	  if(w==0){
		  return -1;
	  }
	  Rect crop = new Rect(w/4,h/4,w/2,h/2);
	  user_poster = user_poster.submat(crop);
	  Imgproc.resize(user_poster, user_poster, new org.opencv.core.Size(), 0.25,0.25,1);


    MatOfKeyPoint userKeyPoints = new MatOfKeyPoint(); 
    MatOfKeyPoint userDescriptors = new MatOfKeyPoint();
    
    List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
    DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
          
    init_keyPoints_descriptors(user_poster, userKeyPoints, userDescriptors, featureDetector, descriptorExtractor);
	  System.out.println("EOKEPNCFZLCN");
	  for (int i=0; i< database_keyPoint.size(); i++){
	      descriptorMatcher.knnMatch(userDescriptors, database_descriptors.get(i), matches, 2);
		  current = check_similarity_surf_2(matches,0.5);
		  System.out.println("i : " + current);
	  if (current>max_sum){
		  max_sum = current;
		  indice = i;
	  }
}
	  return indice;
}

  public static int correspond_film (String image_test, ArrayList<String> list){
	  double max_sum = 0.0, current =0.0;
	  int w,h,indice=-1;
	
	  //load and resize image for speed (cropping)
	  Mat user_poster = Highgui.imread(image_test, CvType.CV_32F);
	  System.out.println("BSize= "+user_poster.height() + "," + user_poster.width());
	  if(user_poster.size().width==0){
		  return -1;
	  }
	  Mat real_poster = Mat.ones(new org.opencv.core.Size(110, 110), CvType.CV_32F);
	  w = (int)user_poster.size().width;
	  h = (int)user_poster.size().height;
	  Rect crop = new Rect(w/4,h/4,w/2,h/2);
	  user_poster = user_poster.submat(crop);
	  Imgproc.resize(user_poster, user_poster, new org.opencv.core.Size(), 0.25,0.25,1);


      MatOfKeyPoint userKeyPoints = new MatOfKeyPoint(); 
      MatOfKeyPoint userDescriptors = new MatOfKeyPoint();
      MatOfKeyPoint realPosterKeyPoints = new MatOfKeyPoint();
      MatOfKeyPoint realPosterDescriptors = new MatOfKeyPoint();
	  
      List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
      DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
    
      
	  FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
      DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
            
      init_keyPoints_descriptors(user_poster, userKeyPoints, userDescriptors, featureDetector, descriptorExtractor);

	  
	  for (int i=0; i< list.size(); i++){
		  real_poster = Highgui.imread(list.get(i), CvType.CV_32F);
	      init_keyPoints_descriptors(real_poster, realPosterKeyPoints, realPosterDescriptors, featureDetector, descriptorExtractor);
	      descriptorMatcher.knnMatch(userDescriptors, realPosterDescriptors, matches, 2);
		  current = check_similarity_surf_2(matches,0.5);
	  if (current>max_sum){
		  max_sum = current;
		  indice = i;
	  }
  }
	  return indice;
  }
  
  
  public  int comparaisonFinal(ArrayList<String> database, String image_test){
	int ret;
	
	//init_database_keyPoints(database, featureDetector, descriptorExtractor,database_keyPoints, database_descriptors);
	ret = correspond_film_v2(image_test, database_keyPoints, database_descriptors, featureDetector, descriptorExtractor);
	
//	  Mat user_poster = Highgui.imread(image_test, CvType.CV_32F);
//	  System.out.println("Size= "+user_poster.height() + "," + user_poster.width());
//	  	ret = correspond_film (image_test, database);
	return ret;
  }
	  
}
  
//  public static void main(String[] args) {
//
//	  ArrayList<String> list = new ArrayList<String>();
//	  list.add("../../images/02.jpg");
//	  list.add("../../images/19.jpg");
//	  list.add("../../images/03.jpg");
//	  
//	  FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.SURF);
//      DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SURF);
//
//      List<MatOfKeyPoint> database_keyPoints = new LinkedList<MatOfKeyPoint>();
//      List<MatOfKeyPoint> database_descriptors = new LinkedList<MatOfKeyPoint>();
//      
//	  init_database_keyPoints(list, featureDetector, descriptorExtractor,database_keyPoints, database_descriptors);
//
//	  System.out.println(correspond_film_v2("testc.jpg", database_keyPoints, database_descriptors, featureDetector, descriptorExtractor));
//  }
//}
