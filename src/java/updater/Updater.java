package updater;

import db4e.Downloader;
import db4e.data.Catalog;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class Updater {

   public static final Logger log = Logger.getLogger( Downloader.class.getName() );

   // Internal status
   private BooleanProperty read = new SimpleBooleanProperty();
   private BooleanProperty done = new SimpleBooleanProperty();
   // Public statuc hooks
   public ReadOnlyBooleanProperty isRead = ReadOnlyBooleanProperty.readOnlyBooleanProperty( read );
   public ReadOnlyBooleanProperty isDone = ReadOnlyBooleanProperty.readOnlyBooleanProperty( done );

   private File basepath;
   private final Downloader main;
   private final Catalog data;
   private Reader reader;
   private Loader loader;
   private Writer writer;

   public Updater ( Downloader main ) {
      this.main = main;
      this.data = main.data;
      read.set( false );
      done.set( false );
   }

   public synchronized void stop () {
      if ( reader == null && writer == null ) return;
      log.fine( "log.updater.stopping" );
      if ( reader != null ) {
         reader.stop();
         reader = null;
      }
      if ( writer != null ) {
         writer.stop();
         writer.waitForDone();
         writer = null;
      }
      log.fine( "log.updater.stopped" );
   }

   public synchronized void setBasePath ( File basepath ) {
      stop();
      data.clear();

      read.set( false );
      reader = Reader.load( data, basepath );
      data.setWriter( writer );

      reader.isRunning.addListener( (prop,old,now) -> { synchronized ( this ) {
         if ( now || reader == null ) return;
         read.set( true );
         if ( ! reader.isInterrupted() ) {
            writer = new Writer( basepath );
            writer.start();
            loader = new Loader();
            loader.start();
         }
         reader = null;
      } } );
      reader.start();
      log.log( Level.CONFIG, "log.updater.rebase", basepath );
   }
}