package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class DetectorController{

	
	
	
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

	}
	
	/*
	 * Variables
	 */
	
	// Video akisini elde etmek icin bir zamanlayici
	private ScheduledExecutorService timer;
	
	//Bir OpenCV Sinifidir sistemdeki kameralara eriþimi saðlar.
	private VideoCapture capture = new VideoCapture();
	
	//Kameranin aktif olup olmadýðýný kontrol eden bayrak deðiþkendir.
	private boolean activeCamera = false;
	
	Rect rectangleb;
	
	Rect rectangleg;

	Rect rectanglef;
	//COLORS LOWER AND UPPER RANGE

	/*------------------------------------------------------------------------------------------------*/
	
	/*
	 * FXML Variables
	 */
	
	@FXML
	private ImageView video_;
	
	/*------------------------------------------------------------------------------------------------*/
	int sayac = 0;
	@FXML
	private void kameraBasla(ActionEvent event) {
		if(!this.activeCamera) {
			
			
			this.capture.open(0);

			if(this.capture.isOpened()) {
				this.activeCamera = true;
				
				
				//(30 frame/sn)
				Runnable imageGrabber = new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						Mat frame = processFrame();
						Image newImage = matToImage(frame);
						updateView(newImage);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(imageGrabber,0, 33,TimeUnit.MILLISECONDS);
			}
		}else {
			//Kamera durum bayragi kapaliya cekiliyor.
			this.activeCamera = false;
			this.stopTimerandCapture();
		}
	}
	
	
	private Mat processFrame() {
		Mat frame = new Mat();
		
		if (this.activeCamera) {
			if (this.capture.isOpened()) {
					
					capture.read(frame);
					Mat blurFrame = new Mat();
					Mat hsvFrame = new Mat();
					Mat maskGreenFrame = new Mat();
					Mat maskBlueFrame = new Mat();
					Mat maskAllFrame = new Mat();
					Mat mask = new Mat();
					Mat fire = new Mat();
					
					Imgproc.blur(frame, blurFrame, new Size(7,7));
					Imgproc.cvtColor(blurFrame, hsvFrame, Imgproc.COLOR_BGR2HSV);
					
					
					Core.inRange(hsvFrame, new Scalar(68.0,131.0,33.0), new Scalar(91.0,255.0,255.0), maskGreenFrame);
					Core.inRange(hsvFrame, new Scalar(92.0,216.0,115.0), new Scalar(117.0,255.0,255.0),maskBlueFrame );
					Core.inRange(hsvFrame, new Scalar(161.0,148.0,181.0), new Scalar(180.0,255.0,255.0), fire);
					
					
					Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
					Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
					
					Imgproc.erode(maskGreenFrame, maskGreenFrame, erodeElement);
					Imgproc.erode(maskBlueFrame, maskBlueFrame, erodeElement);
					Imgproc.erode(fire,fire,erodeElement);
					
					
					Imgproc.dilate(maskBlueFrame, maskBlueFrame, dilateElement);
					Imgproc.dilate(maskGreenFrame, maskGreenFrame, dilateElement);
					Imgproc.dilate(fire,fire,dilateElement);
						
					rectangleb =Imgproc.boundingRect(maskBlueFrame);// Kýrmýzý Maskede 1 olan bitlerin x,y,w,h deðerlerini dönderiyor.
					rectangleg =Imgproc.boundingRect(maskGreenFrame);//Yeþil Maskede 1 olan bitlerin x,y,w,h deðerlerini dönderiyor.
					rectanglef = Imgproc.boundingRect(fire);
					
					//System.out.println("X'ler fark : " + String.valueOf(rectangleb.x / rectangleg.x) + " Y'ler farký :" + String.valueOf(rectangleb.y / rectangleg.y) );
				
					
					if(rectanglef.x > 0 && rectanglef.y > 0) {
						System.out.println("Ateþ Ediyor");
					}else if(rectanglef.x == 0 && rectanglef.y== 0 && rectangleb.y + rectangleb.height < rectangleg.y + rectangleg.height && rectangleb.x > rectangleb.x - rectangleb.width && rectangleb.x < rectangleg.x + rectangleb.width && rectangleg.x < rectangleg.x + rectangleg.width && rectangleg.x > rectangleg.x - rectangleg.width &&  rectangleg.x < rectangleb.x+30  && rectangleg.x > rectangleb.x-30 && rectangleb.y < rectangleg.y ) {
						System.out.println("Niþan aldý");
					}else if(rectangleb.y > rectangleg.y && rectangleb.x < rectangleg.x ) {
						DegreeCalculator(rectangleb.x, rectangleb.y, rectangleg.x, rectangleg.y);
					}else if(rectangleb.y > rectangleg.y && rectangleb.x > rectangleg.x) {
						DegreeCalculator(rectangleb.x, rectangleb.y, rectangleg.x, rectangleg.y);
					}else if(rectangleb.y < rectangleg.y && rectangleb.x < rectangleg.x) {
						DegreeCalculator(rectangleb.x, rectangleb.y, rectangleg.x, rectangleg.y);
					}else if(rectangleb.y < rectangleg.y && rectangleb.x > rectangleg.x) {
						DegreeCalculator(rectangleb.x, rectangleb.y, rectangleg.x, rectangleg.y);
					}
					
					
					
					Core.bitwise_xor(maskBlueFrame, maskGreenFrame, mask);
					
					maskAllFrame = findAndDrawBalls(mask, frame);
					
					return maskAllFrame;
					
					
				
			}
		}
		
		return frame;
		
		
	}
	
	private void DegreeCalculator(int x1, int y1, int x2, int y2) {
		double komsu = Math.abs(x1 - x2);
		double karsi = Math.abs(y1 - y2);
		
		double oran = karsi / komsu;
		double tan = Math.atan(oran);
		double aci = Math.toDegrees(tan);
		
		if(y1 > y2 && x1 < x2) {
			System.out.println("Sol yukarý " + aci  +" ile bakýyor." );
		}else if (y1 < y2 && x1 < x2) {
			System.out.println("Sol aþaðý " + aci  +" ile bakýyor." );
		}else if(y1 > y2 && x1 > x2) {
			System.out.println("Sað yukarý " + aci  +" ile bakýyor." );
		}else if(y1 < y2 && x1 > x2) {
			System.out.println("Sað aþaðý " + aci  +" ile bakýyor." );
		}
	}
	
	private Mat findAndDrawBalls(Mat maskedImage, Mat frame)
	{
		// init
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		
		// find contours
		Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
		
		// if any contour exist...
		if (hierarchy.size().height > 0 && hierarchy.size().width > 0)
		{
			// for each contour, display it in blue
			for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0])
			{
				Imgproc.drawContours(frame, contours, idx, new Scalar(250, 0, 0));
			}
		}
		
		return frame;
	}
	
	private void stopTimerandCapture() {
		// TODO Auto-generated method stub
		if (this.timer != null && !this.timer.isShutdown()) {
			try {
				
				//Timer durduruluyor.
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
				
				
			} catch (Exception e) {
				// TODO: handle exception
				System.err.println("Kamera kapatýlamadý. Bir hata oluþtu : " + e);
			}
		}
		
		if(this.capture.isOpened()) {
			//capture degiskeni icerisinde ki kamera serbest býrakýlýyor. Yani kullanýmdan dusuyor.
			this.capture.release();
		}
	}


	private void updateView(Image image) {
		onFXThread(video_.imageProperty(), image);
	}

	
	//JavaFx'te çalisan platformda ki bileseni güncellemek icin
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	public Image  matToImage(Mat frame) {

		try {
			return SwingFXUtils.toFXImage(matToBuffreadImage(frame), null);
		} catch (Exception e) {
			
			return null;
		}
			
	}


	private BufferedImage matToBuffreadImage(Mat frame) {
		
		BufferedImage bfImage = null;
		
		int height = frame.height(), width = frame.width(), channels = frame.channels();
		
		byte[] srcPixels = new byte[width * height * channels];
		
		frame.get(0, 0,srcPixels);
		
		if (frame.channels() > 1) {
			bfImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		} else {
			bfImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		
		final byte[] hedef = ((DataBufferByte) bfImage.getRaster().getDataBuffer()).getData();
		System.arraycopy(srcPixels, 0, hedef, 0, srcPixels.length);
		
		return bfImage;
	}
	
	protected void setClosed()
	{
		this.stopTimerandCapture();
	}


	
}
