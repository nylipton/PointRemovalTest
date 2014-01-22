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
		int WIDTH = 1920/3, HEIGHT = 1080/3;
		int POINTS = 2000 ;
		int DIST = 10 ;
		int CLUSTERS = 15 ;
		new PointRemovalTest( WIDTH, HEIGHT, POINTS, DIST, CLUSTERS ) ;
	}
	
	public PointRemovalTest( int width, int height, int points, int distance, int clusters )
	{
		Dataset dataset = new DefaultDataset( ) ;
//		Clusterer clusterer = new DensityBasedSpatialClustering( .009, 2 ) ;
		Clusterer clusterer = new KMeans( clusters ) ;
//		Clusterer clusterer = new KMedoids( ) ;
//		Clusterer clusterer = new SOM( ) ;
		nf.setMinimumFractionDigits( 4 ) ;
		
		JFrame frame = new JFrame( "Clustering Demo" ) ;
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		frame.setMinimumSize( new Dimension( width, height ) );
		frame.setVisible( true ) ;
		Cursor c = frame.getCursor( ) ;
		frame.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
		System.out.println( "calculating data..." );
		for( int i = 0 ; i < points ; i++ )
		{
			addData( dataset, width, height ) ;
//			cluster( clusterer, dataset ) ;
		}
		System.out.println( "clustering..." );
		Dataset[] data = cluster( clusterer, dataset ) ;
		Dataset[] removeData = findClosestPoints( data, distance ) ;
		System.out.println( "drawing..." ) ;
		draw( frame, dataset, data, removeData ) ;
		frame.setCursor( c ) ; // done
	}
	
	/**
	 * For each dataset, find the points within each cluster that are within a certain distance
	 * 
	 * @param datasets the clusters
	 * @param	the minimum distance to use as a threshold
	 * @return the data points within closest units
	 */
	public Dataset[] findClosestPoints( Dataset[] datasets, int closest )
	{
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
								remove[i].add( inst2.copy( ) ) ;
//							System.out.println( "distance between ("+x1+","+y1+
//									") & ("+x2+","+y2+")="+d );
						}
					}
				}
			}
//			System.out.println( remove[i] );
		}
		double t2 = ( System.currentTimeMillis( ) - t1 ) / 1000d ;
		System.out.println( "finding closest points took " + nf.format( t2 ) + " secs" ) ;
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
	 * @param frame make sure to set the size and make visible outside of this method
	 * @param allData	All of the data points
	 * @param clusterData	All of the sets of clusters
	 * @param removeData	The data points to be "removed"
	 */
	private void draw( 
			JFrame frame, 
			Dataset allData, 
			Dataset[] clusterData,
			Dataset[] removeData )
	{
		FastConvexHull hullExecutor = new FastConvexHull( ) ;
		Graphics g = frame.getGraphics( ) ;
		Font f = g.getFont( ).deriveFont( 8.0f ) ;
		g.setFont( f ) ;
		
		if( allData != null )
		{
			g.setColor( Color.BLACK ) ;
			draw( g, allData, "x", false ) ;
		}
		
		if( clusterData != null )
		{
			g.setColor( Color.RED ) ;
			for( int i = 0 ; i < clusterData.length ; i++ )
			{
//				draw( g, clusterData[i], "x", false ) ;
				
				// draw the hull
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
		
		if( removeData!= null )
		{
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
