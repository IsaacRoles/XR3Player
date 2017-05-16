package xplayer.services;

import java.util.logging.Level;

import application.Main;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import streamplayer.StreamPlayerException;
import xplayer.presenter.XPlayerController;

/**
 * This Service is used to skip the Audio of XPlayer to different time.
 *
 * @author GOXR3PLUS
 */
public class XPlayerSeekService extends Service<Boolean> {
	
	/** The bytes to be skipped */
	long bytes;
	
	/**
	 * I am using this variables when i want to stop the player and go to a
	 * specific time for example at 1 m and 32 seconds :)
	 */
	boolean stopPlayer;
	
	/**
	 * Determines if the Service is locked , if yes it can't be used .
	 */
	private volatile boolean locked;
	
	private final XPlayerController xPlayerController;
	
	/**
	 * Constructor.
	 */
	public XPlayerSeekService(XPlayerController xPlayerController) {
		this.xPlayerController = xPlayerController;
		setOnSucceeded(s -> done());
		setOnFailed(f -> done());
	}
	
	/**
	 * Start the Service.
	 *
	 * @param bytes1
	 *        Bytes to skip
	 * @param stopPlayer1
	 */
	public void startSeekService(long bytes1 , boolean stopPlayer1) {
		if (locked || isRunning() || xPlayerController.getxPlayerModel().songPathProperty().get() == null)
			return;
		
		//System.out.println(bytes)
		
		//StopPlayer
		this.stopPlayer = stopPlayer1;
		
		// Bytes to Skip
		this.bytes = bytes1;
		
		// Create Binding
		xPlayerController.getFxLabel().textProperty().bind(messageProperty());
		xPlayerController.getFxRegion().visibleProperty().bind(runningProperty());
		
		// lock the Service
		locked = true;
		
		// Restart
		restart();
	}
	
	/**
	 * When the Service is done.
	 */
	private void done() {
		
		// Remove the unidirectional binding
		xPlayerController.getFxLabel().textProperty().unbind();
		xPlayerController.getFxRegion().visibleProperty().unbind();
		xPlayerController.getFxRegion().setVisible(false);
		
		// Stop disc dragging!
		xPlayerController.discIsDragging = false;
		
		// Put the appropriate Cursor
		xPlayerController.getDisc().getCanvas().setCursor(Cursor.OPEN_HAND);
		
		// Recalculate Angle and paint again Disc
		xPlayerController.getDisc().calculateAngleByValue(xPlayerController.getxPlayerModel().getCurrentTime(), xPlayerController.getxPlayerModel().getDuration(),
				true);
		xPlayerController.getDisc().repaint();
		
		// unlock the Service
		locked = false;
		
	}
	
	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				boolean succeded = true;
				
				// ----------------------- Seek the Media
				updateMessage("Skipping the Audio");
				
				//Stop?
				if (stopPlayer)
					xPlayerController.getxPlayer().stop();
				
				// GO
				// if (bytes != 0) { // and xPlayer.isPausedOrPlaying())
				Main.logger.info("Seek Service Started..");
				
				// CurrentTime
				xPlayerController.getxPlayerModel().setCurrentTime(xPlayerController.getxPlayerModel().getCurrentAngleTime());
				
				try {
					xPlayerController.getxPlayer().seek(bytes);
				} catch (StreamPlayerException ex) {
					xPlayerController.logger.log(Level.WARNING, "", ex);
					succeded = false;
				}
				// }
				
				// ----------------------- Play Audio
				if (!xPlayerController.getxPlayer().isPausedOrPlaying()) {
					xPlayerController.getxPlayer().play();
					//xPlayer.pause();
				}
				
				// ----------------------- Configuration
				updateMessage("Applying Settings ...");
				
				// Configure Media Settings
				if (xPlayerController.getxPlayer().isPausedOrPlaying())
					xPlayerController.configureMediaSettings(true);
				
				return succeded;
			}
			
		};
	}
	
}