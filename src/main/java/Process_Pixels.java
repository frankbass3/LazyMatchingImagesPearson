/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * ProcessPixels
 *
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author The Fiji Team
 */
public class Process_Pixels implements PlugInFilter {
	protected static ImagePlus image;
	protected static ImagePlus imagePattern;
	protected ImageProcessor ipattern;
	protected ImageProcessor ipic;
	// image property members
	private int width;
	private int height;
	
	private int patternW;
	private int patternH;
	// plugin parameters
	public double value;
	public String name;

	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		//image = imp;
		return DOES_8G | DOES_16 | DOES_32 | DOES_RGB | NO_IMAGE_REQUIRED;
	}

	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		ipic = image.getProcessor();
		ipattern = imagePattern.getProcessor();
		
		// get width and height
		width = ipic.getWidth();
		height = ipic.getHeight();
		patternH = ipattern.getHeight();
		patternW = ipattern.getWidth();
		if (showDialog()) {
			process(ipic,ipattern);
			image.updateAndDraw();
		}
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Process pixels");

		// default value is 0.00, 2 digits right of the decimal point
		gd.addNumericField("value", 0.00, 2);
		gd.addStringField("method", "Pearson Cross Correlation");

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		value = gd.getNextNumber();
		name = gd.getNextString();

		return true;
	}

	/**
	 * Process an image.
	 *
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 *
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image,ImageProcessor pattern) {
		// slice numbers start with 1 for historical reasons
		for (int i = 1; i <= image.getStackSize(); i++)
			process(image.getStack().getProcessor(i),pattern);
	}

	// Select processing method depending on image type
	public void process(ImageProcessor ip,ImageProcessor pattern) {
		int type = image.getType();
		if (type == ImagePlus.GRAY8)
			process( (byte[]) ip.getPixels(), (byte[])pattern.getPixels(),ip );
		else if (type == ImagePlus.GRAY16)
			process( (short[]) ip.getPixels() );
		else if (type == ImagePlus.GRAY32)
			process( (float[]) ip.getPixels() );
		else if (type == ImagePlus.COLOR_RGB)
			process( (int[]) ip.getPixels() );
		else {
			throw new RuntimeException("not supported");
		}
	}

	
	// processing of GRAY8 images
	/* 
	4 , 6
	
	[1,1,1,1,1,1,2,1,1,1,1,1,3,1,1,1,1,1,4,1,1,1,1,1]
	
	[
	1,1,1,1,1,1
	2,1,1,1,1,1
	3,1,1,1,1,1
	4,1,1,1,1,1
	]
	
	*/
	public void process(byte[] pixels,byte[] pattern, ImageProcessor ip) {
		
		// get the subImage of the size of the pattern 
		// and calculate the match
		float patternMean = getMean(pattern,patternW,patternH);
		float patternStd = getStd(patternMean,pattern,patternW,patternH);
		
		 
		ImageProcessor  stored = null;
		float maxx = 0;
		for (int y=0; y < height-patternH; y++) {
			for (int x=0; x < width-patternW; x++) {
			    
				//ImageProcessor s = getTheRoi(ip, patternH,patternW , x,y );
				
				ip.setRoi(x,y,patternW,patternH);
				ImageProcessor s = ip.crop(); 
			    
				byte[] subImage = (byte[])s.getPixels();
				
				float currentMean = getMean(subImage,s.getWidth(),s.getHeight());
				float currentStd = getStd(currentMean,subImage,s.getWidth(),s.getHeight());
				int currentH = s.getHeight();
				int currentW = s.getWidth();
				float corr=(float) 0.0;
				for (int yy=0; yy < currentH; yy++) {
					for (int xx=0; xx < currentW; xx++) {
						// process each pixel of the line
						// example: add 'number' to each pixel
						corr+=  ((pixels[xx + yy * currentW] - currentMean) * 
									 (pattern[xx + yy * currentW]) - patternMean) * 
									 (1 / (currentStd*patternStd)) ;
						
					}
				} 
				if((maxx < corr)&&(Float.isInfinite(corr)==false)){
					maxx = corr;
					stored = (ImageProcessor) s.duplicate();
				}
				IJ.write(corr + "");
				 
				
			}
		}
		 
		ImagePlus createImage = new ImagePlus("New image", stored); 
		createImage.show(); 
		
		IJ.write(maxx + "maxx");
	}

	// processing of GRAY16 images
	public void process(short[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (short)value;
			}
		}
	}

	// processing of GRAY32 images
	public void process(float[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (float)value;
			}
		}
	}

	// processing of COLOR_RGB images
	public void process(int[] pixels) {
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				pixels[x + y * width] += (int)value;
			}
		}
	}

	public void showAbout() {
		IJ.showMessage("ProcessPixels",
			"a template for processing each pixel of an image"
		);
	}
	float getMean(byte[] dataset,int width,int height ){
		double mValue=0;
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				mValue += dataset[x + y * width];
			}
		}
		return (float) (mValue/dataset.length); 
	}

	float getStd(float mValue,byte[] dataset,int width,int height){
		float sValue=0;
		if (dataset.length==1) {return (float) (sValue);}
		else{
			
			for (int y=0; y < height; y++) {
				for (int x=0; x < width; x++) {
					// process each pixel of the line
					// example: add 'number' to each pixel
					byte b = dataset[x + y * width];
					float d = mValue - b;
					if(d > (float)0) 
					sValue += Math.sqrt(d);
				}
			} 
			return (float) (Math.sqrt(sValue/(dataset.length-1)));
		}
	}
	
	
	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Process_Pixels.class;
		
		//Class<?> clazz = cvMatch_Template.class;
		
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		image = IJ.openImage("C:/Users/TEMP/Downloads/training/goodsample/simle/1.tif");
		imagePattern = IJ.openImage("C:/Users/TEMP/Downloads/training/goodsample/simle/2.tif");
		
		image.show();
		//imagePattern.show();
		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
