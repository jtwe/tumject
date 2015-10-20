package mtg;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import javax.imageio.ImageIO;

public class TableDrawing {
//smolW = 223, smolH = 310, bigW = smolW * 4.5 = 1003.5, bigH = 5 * smolH / 3 = 516

	private boolean outlines = false;
	private boolean verbose = false;
	private boolean fancyTransformExport = false;
	
	private String fileDir;
	private String bgFile;
	private CardDisplayInfo[] cardInfos;
	
	public TableDrawing(String fileDir, String bgFile, CardDisplayInfo[] cardInfos) {
		this.fileDir = fileDir;
		this.bgFile = bgFile;
		this.cardInfos = cardInfos;
	}

	public String generateImage() throws IOException {
		BufferedImage baseImg = ImageIO.read(new File(fileDir + bgFile));

		Random r = new Random();
		double thetaJitter = 0.5d;
//		double thetaJitter = 0.0d;
		
		int minX = Integer.MAX_VALUE, maxX = 0, minY = Integer.MAX_VALUE, maxY = 0;
		for (int i=0; i<cardInfos.length; i++) {
			int x = cardInfos[i].getDefX(), y = cardInfos[i].getDefY();
			if (x<minX) minX = x;
			if (x>maxX) maxX = x;
			if (y<minY) minY = y;
			if (y>maxY) maxY = y;
		}
		int bgW = minX+maxX, bgH = minY+maxY;
		
		BufferedImage finalImg = new BufferedImage(bgW, bgH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = finalImg.createGraphics();

		System.out.println("generateImage: " + baseImg.getWidth() + ", " + minX + ", " + maxX + ", " + bgW);
		int bgX = 0;
		if (baseImg.getWidth()>bgW) bgX = -r.nextInt(baseImg.getWidth()-bgW);
		int bgY = 0;
		if (baseImg.getHeight()>bgH) bgY = -r.nextInt(baseImg.getHeight()-bgH);
		g2d.drawImage(baseImg, bgX, bgY, null);

		AffineTransform orig = g2d.getTransform();

		for (int i=0; i<cardInfos.length; i++) {
			BufferedImage smolImage = ImageIO.read(new File(fileDir + "temp/" + cardInfos[i].getFileName() + ".jpg"));
			smolImage = trimCorners(smolImage);
			int smolW = smolImage.getWidth(), smolH = smolImage.getHeight();
			int x = cardInfos[i].getDefX() - smolW/2, y = cardInfos[i].getDefY() - smolH/2;
			int xJitter = smolW/5, yJitter = smolH/5;
//			int xJitter = 0, yJitter = 0;

			if (outlines) {
				g2d.setTransform(orig);
				g2d.setColor(Color.WHITE);
				g2d.drawRect(x,  y,  smolW,  smolH);
			}

			double thetaDelta = (r.nextDouble()*thetaJitter) - (thetaJitter/2.0d);
			int xDelta = r.nextInt(xJitter+1) - (xJitter/2);
			int yDelta = r.nextInt(yJitter+1) - (yJitter/2);

			AffineTransform rotate = new AffineTransform();
			rotate.translate(smolW/2, smolH/2);
			rotate.rotate(cardInfos[i].getTheta() + thetaDelta, smolW/2, smolH/2);
			//				rotate.translate(-smolW/2, -smolH/2);
			if (verbose) System.out.println(i + ", " + (cardInfos[i].getTheta() + thetaDelta) + ", " + (x + xDelta + smolW/2) + ", " + (y + yDelta + smolH/2));
			//				g2d.setTransform(rotate);

			AffineTransformOp op = new AffineTransformOp(rotate, AffineTransformOp.TYPE_BILINEAR);
			//				BufferedImage rotImage = new BufferedImage(Math.max(smolW,  smolH)*2, Math.max(smolW,  smolH)*2, BufferedImage.TYPE_INT_ARGB);
			//				op.filter(smolImage, rotImage);
			BufferedImage rotImage = op.filter(smolImage, null);
			if (fancyTransformExport) {
				ImageIO.write(rotImage, "jpg", new File(fileDir + "temp/x" + cardInfos[i].getFileName() + ".jpg" ));
			}

			g2d.drawImage(rotImage, null, x+xDelta-smolW/2, y+yDelta-smolH/2);

		}

		String outputFile = fileDir + "output/total_";
		for (int i=0; i<cardInfos.length; i++) outputFile = outputFile + (i==0?"":"_") + cardInfos[i].getFileName();
		outputFile = outputFile + ".png";
		ImageIO.write(finalImg, "png", new File(outputFile));
		return outputFile;
	}
	
	private static BufferedImage trimCorners(BufferedImage smolImage) {
		return trimCorners(smolImage, false);
	}
	
	private static BufferedImage trimCorners(BufferedImage smolImage, boolean verbose) {
		BufferedImage bi = new BufferedImage(smolImage.getWidth(), smolImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		g2d.drawImage(smolImage, null, 0, 0);

		int w = smolImage.getWidth(), h = smolImage.getHeight();
		
		for (int x = 0; x < w; x++) {
			for (int y = 0; y < h/2; y++) {
				int p = bi.getRGB(x, y);
				int brite = 0;
				if (p==0) {
					brite = 255*3;
				} else {
					if (verbose) System.out.print("[" + Integer.toHexString(p) + "]");
					for (int i=0; i<3; i++) {
						brite+=(p&255);
						p = p>>8;
					}
				}
				if (brite>500) {
					bi.setRGB(x, y, 0x00ffffff);
					if (verbose) System.out.print("(" + brite + ")");
					if (verbose) System.out.print(".");
				} else {
					if (x==0 || x==w-1 || y==0 || y==h-1) {
						bi.setRGB(x, y, 0x3fffffff & p);
					}
					if (verbose) System.out.println(x + ", " + y + ", " + brite + "/");
					break;
				}
			}
		}
		
		for (int x = 0; x < w; x++) {
			for (int y = h-1; y > h/2; y--) {
				int p = bi.getRGB(x, y);
				int brite = 0;
				if (p==0) {
					brite = 255*3;
				} else {
					if (verbose) System.out.print("[" + Integer.toHexString(p) + "]");
					for (int i=0; i<3; i++) {
						brite+=(p&255);
						p = p>>8;
					}
				}
				if (brite>500) {
					bi.setRGB(x, y, 0x00ffffff);
					if (verbose) System.out.print("(" + brite + ")");
					if (verbose) System.out.print(".");
				} else {
					if (verbose) System.out.println(x + ", " + y + ", " + brite + "/");
					break;
				}
			}
		}

		return bi;
	}
	
	public static void main(String[] args) throws IOException {
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));

		BufferedImage firstImg = ImageIO.read(new File(prop.getProperty("mtg.directory") + "images/" + prop.getProperty("background.image")));
		int bigW = firstImg.getWidth(), bigH = firstImg.getHeight();
		Random r = new Random();

		int smolW = bigW/6 + r.nextInt(bigW/3);
		int smolH = bigH/6 + r.nextInt(bigH/3);
		
		smolW = 1000;
		smolH = 800;
		
		BufferedImage finalImg = new BufferedImage(smolW, smolH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = finalImg.createGraphics();
		
		int x = -r.nextInt(bigW-smolW);
		int y = -r.nextInt(bigH-smolH);

//		x = -500;
//		y = -400;
		
		g2d.drawImage(firstImg, x, y, null);
		
		ImageIO.write(finalImg, "png", new File("/Users/martella/Documents/games/mtg/images/t_" + smolW + "_" + smolH + "_" + x + "_" + y + ".png"));
	}
	
/*
	public static void mainQ(String[] args) throws IOException {
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));

		String fileDir = prop.getProperty("mtg.directory") + "images/";
		String[] cardFiles = {"386633.jpg"};

		for (int i=0; i<cardFiles.length; i++) {
			BufferedImage smolImage = ImageIO.read(new File(fileDir + cardFiles[i]));
			smolImage = trimCorners(smolImage, true);
			ImageIO.write(smolImage, "png", new File(fileDir + cardFiles[i] + ".png"));
		}
	}

	public static void main3(String[] args) throws IOException {
		boolean outlines = false, verbose = false, fancyTransforms = true, fancyTransformExport = false;

		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));

		String fileDir = prop.getProperty("mtg.directory") + "images/";
		String bgFile = "the_table.png";
		String[] cardFiles = {"20842.jpg", "78965.jpg", "394005.jpg"};
		
		BufferedImage finalImg = ImageIO.read(new File(fileDir + bgFile));
		Graphics2D g2d = finalImg.createGraphics();

		int bigW = finalImg.getWidth(), bigH = finalImg.getHeight();
		
		double[] thetas = {-0.2d, 0.0d, 0.2d};
//		double[] thetas = {0.0d, 0.0d, 0.0d};
		int[] defX = {bigW/4, 2*bigW/4, 3*bigW/4};
		int[] defY = {bigH/2,   bigH/2,   bigH/2};
		
		Random r = new Random();
		double thetaJitter = 0.5d;
//		double thetaJitter = 0.0d;
		
		AffineTransform orig = g2d.getTransform();
		
		for (int i=0; i<3; i++) {
			BufferedImage smolImage = ImageIO.read(new File(fileDir + cardFiles[i]));
			smolImage = trimCorners(smolImage);
			int smolW = smolImage.getWidth(), smolH = smolImage.getHeight();
			int x = defX[i] - smolW/2, y = defY[i] - smolH/2;
//			int xJitter = smolW/5, yJitter = smolH/5;
			int xJitter = 0, yJitter = 0;

			if (outlines) {
				g2d.setTransform(orig);
				g2d.setColor(Color.WHITE);
				g2d.drawRect(x,  y,  smolW,  smolH);
			}

			double thetaDelta = (r.nextDouble()*thetaJitter) - (thetaJitter/2.0d);
			int xDelta = r.nextInt(xJitter+1) - (xJitter/2);
			int yDelta = r.nextInt(yJitter+1) - (yJitter/2);
			
			if (fancyTransforms) {
				AffineTransform rotate = new AffineTransform();
				rotate.translate(smolW/2, smolH/2);
				rotate.rotate(thetas[i] + thetaDelta, smolW/2, smolH/2);
//				rotate.translate(-smolW/2, -smolH/2);
				if (verbose) System.out.println(i + ", " + (thetas[i] + thetaDelta) + ", " + (x + xDelta + smolW/2) + ", " + (y + yDelta + smolH/2));
//				g2d.setTransform(rotate);

				AffineTransformOp op = new AffineTransformOp(rotate, AffineTransformOp.TYPE_BILINEAR);
//				BufferedImage rotImage = new BufferedImage(Math.max(smolW,  smolH)*2, Math.max(smolW,  smolH)*2, BufferedImage.TYPE_INT_ARGB);
//				op.filter(smolImage, rotImage);
				BufferedImage rotImage = op.filter(smolImage, null);
				if (fancyTransformExport) {
					ImageIO.write(rotImage, "jpg", new File(fileDir + "x" + cardFiles[i]));
				}

				g2d.drawImage(rotImage, null, x+xDelta-smolW/2, y+yDelta-smolH/2);
			} else {
				AffineTransform rotate = new AffineTransform();
				rotate.rotate(thetas[i] + thetaDelta, x + xDelta + smolW/2, y + yDelta + smolH/2);
				if (verbose) System.out.println(i + ", " + (thetas[i] + thetaDelta) + ", " + (x + xDelta + smolW/2) + ", " + (y + yDelta + smolH/2));
				g2d.setTransform(rotate);
				
				g2d.drawImage(smolImage, null, x+xDelta, y+yDelta);
			}
			
		}
		
		ImageIO.write(finalImg, "png", new File(fileDir + "cards.png"));
		
	}

	public static void main2(String[] args) {
		BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		
		g2d.setColor(Color.RED);
		g2d.fillRect(-1,  -1,  2,  2);
		System.out.println("Red: " + Integer.toHexString(bi.getRGB(0,  0)));
		
		g2d.setColor(Color.GREEN);
		g2d.fillRect(-1,  -1,  2,  2);
		System.out.println("Green: " + Integer.toHexString(bi.getRGB(0,  0)));
		
		g2d.setColor(Color.BLUE);
		g2d.fillRect(-1,  -1,  2,  2);
		System.out.println("Blue: " + Integer.toHexString(bi.getRGB(0,  0)));
	}
	
	public static void maine(String[] args) throws IOException {
		String propertyFileName = args[0];
		Properties prop = new Properties();
		prop.load(new FileReader(propertyFileName));

		String fileDir = prop.getProperty("mtg.directory") + "images/";
		
		boolean outlines = true, verbose = true;
		int bigW = 450, bigH = 250, smolW = 100, smolH = 150;
		
		Color[] colors = {Color.RED, Color.GREEN, Color.BLUE};
		double[] thetas = {-0.2d, 0.0d, 0.2d};
		int[] x = {bigW/4-smolW/2, 2*bigW/4-smolW/2, 3*bigW/4-smolW/2};
		int[] y = {bigH/2-smolH/2,   bigH/2-smolH/2,   bigH/2-smolH/2};
		
		Random r = new Random();
		double thetaJitter = 0.5d;
		int xJitter = 20;
		int yJitter = 30;
		
		BufferedImage finalImg = new BufferedImage(bigW, bigH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = finalImg.createGraphics();
		
		g2d.setColor(Color.GRAY);
		g2d.fillRect(0, 0, bigW, bigH);
		
		AffineTransform orig = g2d.getTransform();
		
		for (int i=0; i<3; i++) {
			BufferedImage smolImage = new BufferedImage(smolW, smolH, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2dsmol = smolImage.createGraphics();
			g2dsmol.setColor(colors[i]);
			g2dsmol.fillRect(0,  0,  smolW,  smolH);
			g2dsmol.setColor(Color.BLACK);
			g2dsmol.drawRect(0,  0,  2,  2);
			
			if (outlines) {
				g2d.setTransform(orig);
				g2d.setColor(Color.WHITE);
				g2d.drawRect(x[i],  y[i],  smolW,  smolH);
			}

			double thetaDelta = (r.nextDouble()*thetaJitter) - (thetaJitter/2.0d);
			int xDelta = r.nextInt(xJitter+1) - (xJitter/2);
			int yDelta = r.nextInt(yJitter+1) - (yJitter/2);
			
			AffineTransform rotate = new AffineTransform();
			rotate.rotate(thetas[i] + thetaDelta, x[i] + xDelta + smolW/2, y[i] + yDelta + smolH/2);
			if (verbose) System.out.println(i + ", " + (thetas[i] + thetaDelta) + ", " + (x[i] + xDelta + smolW/2) + ", " + (y[i] + yDelta + smolH/2));
			g2d.setTransform(rotate);
			
			g2d.drawImage(smolImage, null, x[i]+xDelta, y[i]+yDelta);
		}
		
		ImageIO.write(finalImg, "png", new File(fileDir + "cards.png"));
		
	}
*/
}
