package com.daniellipton.javamltest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

/**
 * Divide and conquer to remove superfluous data points on a 2D field
 * 
 * @author daniellipton
 *
 */
public class PointRemovalTest
{	
	private NumberFormat nf = NumberFormat.getNumberInstance( ) ;
	
	public static void main( String[] args )
	{
		int WIDTH = 1920/3, HEIGHT = 1080/3 ;
		int POINTS = 500 ; // points to draw on the screen
		int DIST = 10 ; // minimum distance between points
		int CLUSTERS = POINTS/50 ; // clusters to distribute the data
		PointRemovalTest test = new PointRemovalTest( ) ;
		test.execute( true, WIDTH, HEIGHT, POINTS, DIST, CLUSTERS ) ;
	}
	
	public PointRemovalTest( )
	{
		nf.setMinimumFractionDigits( 4 ) ;
	}
	
	public void execute( boolean draw, int width, int height, int points, int distance, int clusters )
	{
		Dataset dataset = new DefaultDataset( ) ;
		Clusterer dbsc = new DensityBasedSpatialClustering( .05, 2 ) ;
		Clusterer clusterer = new KMeans( clusters ) ;
//		Clusterer clusterer = new KMedoids( ) ;
//		Clusterer clusterer = new SOM( ) ;
		
		JFrame frame = null ;
		Cursor c = null ;
		PaintWindowComponent pwc = null ;
		ControlsPanel controlsPanel = null ;
		if( draw )
		{
			// set up window frames
			frame = new JFrame( "Point Density Test" ) ;
			frame.getContentPane( ).setLayout( new BorderLayout( ) );
			pwc = new PaintWindowComponent( ) ;
			controlsPanel = new ControlsPanel( pwc ) ;

			frame.getContentPane( ).add( pwc, BorderLayout.CENTER ) ;
			frame.getContentPane( ).add( controlsPanel, BorderLayout.NORTH ) ;
			frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			pwc.setPreferredSize( new Dimension( width, height ) ) ;
			pwc.setBorder( BorderFactory.createEtchedBorder( ) );
			frame.pack( ) ;

			frame.setVisible( true ) ;
			c = frame.getCursor( ) ;
			frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) ) ;
			controlsPanel.setEnabled( false ) ;
		}
		
		// calculate random data points around clusters
		calculateRandomData( width, height, points, clusters, dataset ) ;
		
		// find points to remove
		System.out.println( "clustering..." ) ;
		System.out.println( "cluster neighbors..." ) ;
		Dataset[] data = cluster( clusterer, dataset ) ;
		System.out.println( "determine dbsc removal..." ) ;
		long t1 = System.currentTimeMillis( ) ;
		ArrayList<Dataset> removals = new ArrayList<Dataset>( ) ;
		for( int i = 0 ; i < data.length ; i++ )
		{
			 Dataset[] tmpDatasets = dbsc.cluster( data[i] ) ;
			 for( int j = 0 ; j < tmpDatasets.length ; j++ )
				 removals.add( tmpDatasets[j] ) ;
		}
		Dataset[] totalRemoveData = removals.toArray( new Dataset[removals.size( )] ) ;
		double t2 = (System.currentTimeMillis( ) - t1 ) / 1000d ;
		System.out.println( "dbsc clustering time="+nf.format( t2 ) ) ;
		
		/*
		Dataset[] removeData = findClosestPoints( data, distance ) ;
		// one more time on the remaining data w/o clusters
		// first create the set of all data not removed
		Dataset showData = new DefaultDataset( ) ;
		Iterator<Instance> iter = dataset.iterator( ) ;
		while( iter.hasNext( ) )
		{
			Instance d = iter.next( ) ;
			boolean remove = false ;
			for( int i = 0 ; i < removeData.length ; i++ )
			{
				if( removeData[i].contains( d ) )
					remove = true ;
			}
			if( !remove )
				showData.add( d.copy( ) ) ;
		}
		Dataset[] removeData2 = findClosestPoints( new Dataset[]{ showData }, distance ) ;
		Dataset[] totalRemoveData = new Dataset[removeData.length + removeData2.length] ;
		System.arraycopy( removeData, 0, totalRemoveData, 0, removeData.length ) ;
		System.arraycopy( removeData2, 0, totalRemoveData, removeData.length, removeData2.length ) ;
		*/
		
		if( draw )
		{
			// draw all of the data
			System.out.println( "drawing..." ) ;
			pwc.setData( dataset, data, totalRemoveData ) ;
//			shownDataFrame.setCursor( c ) ;
//			calcDataFrame.setCursor( c ) ; 
			frame.setCursor( c ) ; // done
			controlsPanel.setEnabled( true ) ;
			pwc.repaint( ) ;
		}
	}

	/**
	 * Calculates pseudo-random data. The latStdev and longStdev tend to do better with smaller numbers 
	 * given more data points
	 */
	private void calculateRandomData( int width, int height, int points, int clusters, Dataset dataset )
	{
		System.out.println( "calculating data..." );
		double latStdev = height / 30, longStdev = width / 30 ;
		Random rand = new Random( System.currentTimeMillis( ) ) ;
		for( int i = 0 ; i < clusters ; i++ )
		{
			double longt = rand.nextInt( width ) ;
			double lat = rand.nextInt( height )  ;
			double[] vals = { longt, lat } ;
			Instance inst = new DenseInstance( vals ) ;
			dataset.add( inst ) ;
			for( int j = 0 ; j < ( points/clusters - 1 ) ; j++ )
			{
				double y = Math.max( 1, Math.min( height, ( int ) lat + rand.nextGaussian( ) * latStdev ) ) ;
				double x = Math.max( 1, Math.min( width, ( int ) longt + rand.nextGaussian( ) * longStdev ) ) ;
				vals = new double[]{ x, y } ;
				inst = new DenseInstance( vals ) ;
				dataset.add( inst ) ;
			}
		}
	}
	
	/**
	 * For each dataset, find the points within each cluster that are within a certain distance.<p>
	 * Brute-force approach!
	 * 
	 * @param datasets the clusters
	 * @param	the minimum distance to use as a threshold
	 * @return the data points within closest units
	 */
	public Dataset[] findClosestPoints( Dataset[] datasets, int closest )
	{
		int pointsRemoved = 0 ;
		Dataset[] remove = new Dataset[datasets.length] ;
		long t1 = System.currentTimeMillis( ) ;
		for( int i = 0 ; i < datasets.length ; i++ )
		{
			Dataset ds = datasets[i] ;
			remove[i] = new DefaultDataset( ) ;
			for( int j = 0 ; j < ds.size( ) - 1 ; j++ )
			{
				Instance inst = ds.get( j ) ;
				if( !remove[i].contains( inst ) )
				{
					int x1 = inst.get( 0 ).intValue( ) ;
					int y1 = inst.get( 1 ).intValue( ) ;
					for( int k = j + 1 ; k < ds.size( ) ; k++ )
					{
						Instance inst2 = ds.get( k ) ;
						if( !remove[i].contains( inst2 ) )
						{
							int x2 = inst2.get( 0 ).intValue( ) ;
							int y2 = inst2.get( 1 ).intValue( ) ;
							int d = ( int ) Math.sqrt( 
									Math.pow( x2 - x1, 2 ) +
									Math.pow( y2 - y1, 2 ) ) ;
							if( d <= closest )
							{
								remove[i].add( inst2.copy( ) ) ;
								pointsRemoved++ ;
							}
//							System.out.println( "distance between ("+x1+","+y1+
//									") & ("+x2+","+y2+")="+d );
						}
					}
				}
			}
//			System.out.println( remove[i] );
		}
		double t2 = ( System.currentTimeMillis( ) - t1 ) / 1000d ;
		System.out.println( "finding points to remove took " + nf.format( t2 ) + " secs" ) ;
		System.out.println( "can remove " + pointsRemoved + " points" ) ;
		return remove ;
	}
	
	public void addData( Dataset dataset, int width, int height )
	{
		double lat = ( Math.random( ) * width ) ;
		double longt = ( Math.random( ) * height )  ;
		double[] vals = { lat, longt } ;
		Instance inst = new DenseInstance( vals ) ;
		dataset.add( inst ) ;
	}
	
	private Dataset[] cluster( Clusterer clusterer, Dataset dataset )
	{
		long t = System.currentTimeMillis( ) ;
		Dataset[] dArray = clusterer.cluster( dataset ) ;
		double t2 = (System.currentTimeMillis( ) - t ) / 1000d ;
		System.out.println( "clustering time="+nf.format( t2 ) ) ;
		return dArray ;
	}
	
	/**
	 * Control panel for this display
	 */
	class ControlsPanel
	extends JPanel implements ActionListener
	{
		final String ALL = "all data", CALC = "calculated data", SHOWN = "shown data" ;
		PaintWindowComponent pwc ;
		JRadioButton allButton, calculatedButton, shownButton ;
		
		ControlsPanel( PaintWindowComponent pwc )
		{
			this.pwc = pwc ;
			
			allButton = new JRadioButton( ALL ) ;
			calculatedButton = new JRadioButton( CALC ) ;
			shownButton = new JRadioButton( SHOWN ) ;
			add( allButton ) ;
			add( calculatedButton ) ;
			add( shownButton ) ;
			
			ButtonGroup group = new ButtonGroup( ) ;
			group.add( allButton ) ;
			group.add( calculatedButton ) ;
			group.add( shownButton ) ;
			allButton.setSelected( true ) ;
			
			allButton.addActionListener( this ) ;
			calculatedButton.addActionListener( this ) ;
			shownButton.addActionListener( this ) ;
		}
		
		@Override
		public void actionPerformed( ActionEvent evt )
		{
			JRadioButton btn = ( JRadioButton ) evt.getSource( ) ;
			
			if( btn.getText( ).equals( ALL ) )
				pwc.mode = Mode.all ;
			else if( btn.getText( ).equals( CALC ) )
				pwc.mode = Mode.calc ;
			else pwc.mode = Mode.shown ;
			
			pwc.repaint( ) ;
		}
		
		@Override
		public void setEnabled( boolean enabled )
		{
			super.setEnabled( enabled ) ;
			allButton.setEnabled( enabled ) ;
			calculatedButton.setEnabled( enabled ) ;
			shownButton.setEnabled( enabled );
		}
	}
	
	/**
	 * what to draw
	 */
	enum Mode { all, calc, shown } ;
	
	class PaintWindowComponent
	extends JComponent
	{
		Mode mode = Mode.all ;
		
		Dataset allData ;
		Dataset[] clusterData ;
		Dataset[] removeData ;
		
		@Override
		protected void paintComponent( Graphics g )
		{
			draw( g ) ;
		}

		public void setData( Dataset dataset, Dataset[] data, Dataset[] totalRemoveData )
		{
			this.allData = dataset ;
			this.clusterData = data ;
			this.removeData = totalRemoveData ;
		}
		
		/**
		 * Draws the given data
		 * 
		 * @param allDataFrame make sure to set the size and make visible outside of this method
		 * @param allData	All of the data points
		 * @param clusterData	All of the sets of clusters
		 * @param removeData	The data points to be "removed"
		 */
		private void draw( Graphics g )
		{
			FastConvexHull hullExecutor = new FastConvexHull( ) ;
			Font f = g.getFont( ).deriveFont( 8.0f ) ;
			g.setFont( f ) ;
			
			if( mode == Mode.all && allData != null )
			{
				g.setColor( Color.BLACK ) ;
				Iterator<Instance> iter = allData.listIterator( ) ;
				while( iter.hasNext( ) )
				{
					Instance inst = iter.next( ) ;
					int x = inst.get( 0 ).intValue( ) ;
					int y = inst.get( 1 ).intValue( ) ;
					g.drawString( "x", x, y ) ;
				}
			}
			else if( mode == Mode.shown && allData != null && removeData != null )
			{
				g.setColor( Color.BLACK ) ;
				Iterator<Instance> iter = allData.listIterator( ) ;
				while( iter.hasNext( ) )
				{
					Instance inst = iter.next( ) ;
					int x = inst.get( 0 ).intValue( ) ;
					int y = inst.get( 1 ).intValue( ) ;
					
					// was this datapoint removed? if not then draw it on g
					boolean removed = false ;
					for( int i = 0 ; i < removeData.length ; i++ )
						if( removeData[i].contains( inst ) ) removed = true ;
					
					if( !removed )
						g.drawString( "x", x, y ) ;
				}
			}
			else if( mode == Mode.calc && allData != null && removeData != null && clusterData != null )
			{
				// draw all of the data points
				g.setColor( Color.BLACK ) ;
				Iterator<Instance> iter = allData.listIterator( ) ;
				while( iter.hasNext( ) )
				{
					Instance inst = iter.next( ) ;
					int x = inst.get( 0 ).intValue( ) ;
					int y = inst.get( 1 ).intValue( ) ;
					g.drawString( "x", x, y ) ;
				}
				
				// now draw the hulls
				g.setColor( Color.RED ) ;
				for( int i = 0 ; i < clusterData.length ; i++ )
				{
//					draw( g, clusterData[i], "x", false ) ;
					
					// draw the hull
					if( clusterData[i].size( ) > 1 )
					{
						ArrayList<Point> points = new ArrayList<Point>( clusterData[i].size( ) ) ;
						for( int j = 0 ; j < clusterData[i].size( ) ; j++ )
							points.add( new Point( clusterData[i].get( j ).get( 0 ).intValue( ), 
									clusterData[i].get( j ).get(  1 ).intValue( ) ) ) ;
						ArrayList<Point> hull = hullExecutor.execute( points ) ;
						Point last = hull.get( 0 ) ;
						Point first = last ;
						for( int j = 1 ; j < hull.size( ) ; j++ )
						{
							Point p = hull.get( j ) ;
							g.drawLine( last.x, last.y, p.x, p.y ) ;
							last = p ; 
						}
						g.drawLine( last.x, last.y, first.x, first.y ) ;
					}
				}
				// now draw the removed points
				g.setColor( Color.BLUE ) ;
				for( int i = 0 ; i < removeData.length ; i++ )
					draw( g, removeData[i], "x", false ) ;
			}
		}
		
		private void draw( Graphics g, Dataset dataset, String s, boolean drawline )
		{

			Iterator<Instance> iter = dataset.listIterator( ) ;
			int lastx = -1, lasty = -1 ;
			while( iter.hasNext( ) )
			{
				Instance inst = iter.next( ) ;
				int x = inst.get( 0 ).intValue( ) ;
				int y = inst.get( 1 ).intValue( ) ;
				g.drawString( s, x, y ) ;
				if( drawline && lastx != -1 )
					g.drawLine( x, y, lastx, lasty );
				lastx = x ;
				lasty = y ;
			}
		}
	}
}
