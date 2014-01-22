package com.daniellipton.javamltest;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JFrame;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;

public class PointRemovalTest
{	
	private NumberFormat nf = NumberFormat.getNumberInstance( ) ;
	
	public static void main( String[] args )
	{
		int WIDTH = 1920/3, HEIGHT = 1080/3 ;
		int POINTS = 5000 ; // points to draw on the screen
		int DIST = 30 ; // minimum distance between points
		int CLUSTERS = 10 ; // clusters to distribute the data
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
//		Clusterer clusterer = new DensityBasedSpatialClustering( .009, 2 ) ;
		Clusterer clusterer = new KMeans( clusters ) ;
//		Clusterer clusterer = new KMedoids( ) ;
//		Clusterer clusterer = new SOM( ) ;
		
		JFrame allDataFrame = null, shownDataFrame = null, calcDataFrame = null ;
		Cursor c = null ;
		if( draw )
		{
			// set up window frames
			allDataFrame = new JFrame( "All Data" ) ;
			allDataFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			allDataFrame.setMinimumSize( new Dimension( width, height ) );
			shownDataFrame = new JFrame( "Shown Data" ) ;
			shownDataFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			shownDataFrame.setMinimumSize( new Dimension( width, height ) );
			calcDataFrame = new JFrame( "Calculated Data" ) ;
			calcDataFrame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
			calcDataFrame.setMinimumSize( new Dimension( width, height ) );
			calcDataFrame.setLocation( allDataFrame.getLocation( ).x + width + 10, allDataFrame.getLocation( ).y );
			shownDataFrame.setLocation( allDataFrame.getLocation( ).x, allDataFrame.getLocation( ).y + height + 10 );
			calcDataFrame.setVisible( true ) ;
			allDataFrame.setVisible( true ) ;
			shownDataFrame.setVisible( true );
			c = allDataFrame.getCursor( ) ;
			allDataFrame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) ) ;
			calcDataFrame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) ) ;
			shownDataFrame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) ) ;
		}
		
		// calculate random data points
		System.out.println( "calculating data..." );
		for( int i = 0 ; i < points ; i++ )
			addData( dataset, width, height ) ;
		
		// find points to remove
		System.out.println( "clustering..." );
		Dataset[] data = cluster( clusterer, dataset ) ;
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
		
		if( draw )
		{
			// draw all of the data
			System.out.println( "drawing..." ) ;
			draw( calcDataFrame, allDataFrame, shownDataFrame, dataset, data, totalRemoveData ) ;
			shownDataFrame.setCursor( c ) ;
			calcDataFrame.setCursor( c ) ; 
			allDataFrame.setCursor( c ) ; // done
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
	 * Draws the given data
	 * 
	 * @param allDataFrame make sure to set the size and make visible outside of this method
	 * @param allData	All of the data points
	 * @param clusterData	All of the sets of clusters
	 * @param removeData	The data points to be "removed"
	 */
	private void draw( 
			JFrame calcDataFrame,
			JFrame allDataFrame,
			JFrame shownDataFrame,
			Dataset allData, 
			Dataset[] clusterData,
			Dataset[] removeData )
	{
		FastConvexHull hullExecutor = new FastConvexHull( ) ;
		Graphics allGraphics = allDataFrame.getGraphics( ) ; // show all data points
		Font f = allGraphics.getFont( ).deriveFont( 8.0f ) ;
		allGraphics.setFont( f ) ;
		Graphics shownGraphics = shownDataFrame.getGraphics( ) ; // show w/o removed data
		f = shownGraphics.getFont( ).deriveFont( 8.0f ) ;
		shownGraphics.setFont( f ) ;
		Graphics calcGraphics = calcDataFrame.getGraphics( ) ; // show all data w/ clusters & highlighted removals
		f = calcGraphics.getFont( ).deriveFont( 8.0f ) ;
		calcGraphics.setFont( f ) ;
		
		if( allData != null )
		{
			calcGraphics.setColor( Color.BLACK ) ;
			allGraphics.setColor( Color.BLACK ) ;
			shownGraphics.setColor( Color.BLACK ) ;
//			draw( g, allData, "x", false ) ;
			Iterator<Instance> iter = allData.listIterator( ) ;
			while( iter.hasNext( ) )
			{
				Instance inst = iter.next( ) ;
				int x = inst.get( 0 ).intValue( ) ;
				int y = inst.get( 1 ).intValue( ) ;
				calcGraphics.drawString( "x", x, y ) ;
				allGraphics.drawString( "x", x, y ) ;
				
				// was this datapoint removed? if not then draw it on shownGraphics
				boolean removed = false ;
				for( int i = 0 ; i < removeData.length ; i++ )
					if( removeData[i].contains( inst ) ) removed = true ;
				
				if( !removed )
					shownGraphics.drawString( "x", x, y ) ;
			}
		}
		
		if( clusterData != null )
		{
			calcGraphics.setColor( Color.RED ) ;
			for( int i = 0 ; i < clusterData.length ; i++ )
			{
//				draw( g, clusterData[i], "x", false ) ;
				
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
						calcGraphics.drawLine( last.x, last.y, p.x, p.y ) ;
						last = p ; 
					}
					calcGraphics.drawLine( last.x, last.y, first.x, first.y ) ;
				}
			}
		}
		
		if( removeData!= null )
		{
			calcGraphics.setColor( Color.BLUE ) ;
			for( int i = 0 ; i < removeData.length ; i++ )
				draw( calcGraphics, removeData[i], "x", false ) ;
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
