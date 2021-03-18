import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;
import java.io.File;
import paddle.*;
import canoedb.*;


public class ServePulses {
  public static void main(String[] args) throws Exception {

    PulseServerState c = new PulseServerState( "pulse-data" );
    
    Server testPost 				= new ServerHTTP( c, 9000, "test POST" );
    Server displayLast		 	= new ServerHTTP( c, 9001, "display last pulse" );
    Server display100 			= new ServerHTTP( c, 9002, "display last 100 pulses" );

    Server pulsePost 				= new ServerHTTP( c, 49155, "pulse data POST (byte[])" );
    Server pulsePostTest		= new ServerHTTP( c, 49154, "pulse data POST (hex String)" );

		

    while(true) {
      Thread.sleep(1000);

    }


  }

}


class PulseServerState extends ServerState {
  
  private DummyPulseData dpd = new DummyPulseData();

  private TemplateFile plotlyTemplate;
  private TemplateFile plotlyJS;
  private Database pulseDatabase;
  
  public PulseServerState ( String folderName ) throws Exception {
		plotlyTemplate = new TemplateFile( "plot-template.html", "////" );
		plotlyJS = new TemplateFile( "plotly-latest.min.js", "////" );
  	pulseDatabase = new Database( folderName );
    System.out.println( "PulseServerState initialized!" );
  }
  
  private String arrayIntegerJoin (List<Integer> l) {
  	String str = "";
  	for (Integer i : l) str += ","+i;
  	return str.substring(1);
  }
  
  private String arrayDoubleJoin (List<Double> l) {
  	String str = "";
  	for (Double d : l) str += ","+d;
  	return str.substring(1);
  }
  
  private void addPulse ( int sessionId, PulseData pd ) {
 		Query q = pulseDatabase.query( sessionId );
		//q.transform( "pulses", "Server Timestamp", "TimeStamp" )
		q.input( "pulses", "Server Timestamp", "auto" )
		 .input( "pulses", "Version", String.valueOf(pd.version()) )
		 .input( "pulses", "Capture ID", String.valueOf(pd.captureId()) )
		 .input( "pulses", "FPGA Count", String.valueOf(pd.count()) )
		 .input( "pulses", "Server Count", String.valueOf(pd.actualCount()) )
		 .input( "pulses", "FPGA Timestamp", String.valueOf(pd.timestamp()) )
		 .input( "pulses", "duration", String.valueOf(pd.duration()) )
		 .input( "pulses", "Samples", arrayIntegerJoin( pd.samples() ) )
		 .input( "pulses", "Amplitude", arrayDoubleJoin( pd.amplitude() ) )
		 .execute( true ); // write flag
		System.out.println( q );
	}
	
	public List<TableRow> pulseList ( int sessionId, String queryData ) {
		Query q = pulseDatabase.query( sessionId );
		q.execute( queryData );
		List<TableRow> pulses = new ArrayList<>();
		for (TableRow tr : q.rows( q.db().table("pulses"), "Server Timestamp" )) { // use "Server Timestamp" as the filter
			pulses.add( tr );
		}
		return pulses;
	}

	
  public void respondHTTP ( RequestHTTP req, ResponseHTTP res ) {
  	System.out.println( req.path() );

		////////////////////////// plotly-latest.min.js
  	if ( req.path().toLowerCase().equals("/plotly-latest.min.js") ) {
			res.setBody( plotlyJS.toString() );
			res.setMIME( "text/javascript" );

		////////////////////////// 49154
  	} else if ( req.socket().getLocalPort() == 49154 ) {
  		PulseData pd = new PulseData( req.data(), 16 );
  		addPulse( req.sessionId(), pd );
  		res.setMIME( "text/plain" );
  		res.setBody( "Converted string from hex to bytes:\n"+req.data()+"\n"+pd );
  		
		////////////////////////// 49155
  	} else if ( req.socket().getLocalPort() == 49155 ) {
  		PulseData pd = new PulseData( req.data().getBytes(), 16 );
  		addPulse( req.sessionId(), pd );
  		res.setMIME( "text/plain" );
  		res.setBody( "Thanks Storm!\n\n"+pd );
  		  		
		////////////////////////// dummy data
  	} else if ( req.path().toLowerCase().equals("/dummy") ) {
  		PulseData pd = dpd.spawn();
  		addPulse( req.sessionId(), pd );
  		res.setMIME( "text/plain" );
  		res.setBody( "Dummy data:\n"+pd );
  		
		////////////////////////// 2d
  	} else if ( req.path().toLowerCase().equals("/2d") ) {
   		List<TableRow> pulses = pulseList( req.sessionId(), req.data() );
			String latestPulseTime = pulses.get(pulses.size()-1).read("Server Timestamp");
			String latestPulse = pulses.get(pulses.size()-1).read("Samples");	
			String plotlyDiv = "<table>\n";
			String plotCode = "";
			for (int i=pulses.size()-1; i>=0; i--) {
				String divId = "plot"+i;
				plotlyDiv += "<tr>"+
										"<td width=100><button onClick=\"window.open(encodeURI('data:text/csv;charset=utf-8,"+pulses.get(i).read("Samples")+"'))\">Download CSV</button></td>"+
										"<td width=100>"+pulses.get(i).read("Server Timestamp")+"</td>"+
										"<td width=100>"+pulses.get(i).read("Version")+"</td>"+
										"<td width=100>"+pulses.get(i).read("Capture ID")+"</td>"+
										"<td width=100>"+pulses.get(i).read("FPGA Count")+"</td>"+
										"<td width=100>"+pulses.get(i).read("Server Count")+"</td>"+
										"<td width=100>"+pulses.get(i).read("duration")+"</td>"+
										"<td><div id='"+divId+"'></div></td>"+
										"</tr>"+
										"\n";
				plotCode += "Plotly.newPlot( '"+divId+
										"', [ {type:'scatter', marker:{opacity:'0.2', color:'#0000ff'}, y:["+pulses.get(i).read("Samples")+"]}, {type:'scatter', marker:{opacity:'0.2', color:'#00ffff'}, y:["+pulses.get(i).read("Amplitude")+"]} ], layout );\n";
			}
			plotlyDiv += "</table>";
  		res.setBody(
  			plotlyTemplate
				.replace( "latestPulseTime", latestPulseTime )
				.replace( "latestPulse", latestPulse )
  			.replace( "plotSize", "autosize:false, width:500, height:100," )
  			.replace( "plotlyDiv", plotlyDiv )
				.replace( "plotCode", plotCode
				).toString()
  		);
			
		////////////////////////// latest
  	} else if ( req.path().toLowerCase().equals("/latest") ) {
   		List<TableRow> pulses = pulseList( req.sessionId(), "pulses.Server+Timestamp.Last=" );  		
  		String plotlyData = "";
  		for (TableRow tr : pulses) {
  			plotlyData += "{type:'scatter', marker:{opacity:'0.2', color:'#0000ff'}, y:["+tr.read( "Samples" )+"]},\n";
			}			
			String latestPulseTime = pulses.get(0).read("Server Timestamp");
			String latestPulse = pulses.get(0).read("Samples");  		
			String latestAmplitude = pulses.get(0).read("Amplitude");  		
  		res.setBody(
  			plotlyTemplate
  			.replace( "plotlyDiv", "Latest pulse at: "+latestPulseTime+"<button onClick=\"window.open(encodeURI('data:text/csv;charset=utf-8,"+latestPulse+"'))\">Download CSV</button><br><br><div id='plot_div'></div>" )
  			.replace( "plotSize", "autosize:false, width:1200, height:720," )
				.replace( "latestPulseTime", latestPulseTime )
				.replace( "latestPulse", latestPulse )
				.replace( "plotCode",
					"Plotly.newPlot( 'plot_div', [{type:'scatter', marker:{opacity:'0.2', color:'#0000ff'}, y:["+
					latestPulse +
					"]}," +
					"{type:'scatter', marker:{opacity:'0.2', color:'#00ffff'}, y:["+
					latestAmplitude +
					"]}," +
					" ], layout );\n"
				).toString()
  		);

 		////////////////////////// 3d
  	} else if ( req.path().toLowerCase().equals("/3d") ) {
   		List<TableRow> pulses = pulseList( req.sessionId(), req.data() );
  		String plotlyData = "";
			String latestPulseTime = pulses.get(pulses.size()-1).read("Server Timestamp");
			String latestPulse = pulses.get(pulses.size()-1).read("Samples");	
 			if (pulses.size() > 0) {
 				int x=0;
				for (TableRow pulse : pulses) {
					int y = 0;
					String xStr = "";
					String yStr = "";
	 				for (int i=0; i<pulse.read("Samples").split(",").length; i++) {
//						xStr += "\""+pulse.read("Server Timestamp")+"\",";
						xStr += x+",";
						yStr += (y++)+",";
					}
					x++;
					plotlyData += 	"{\n" +
													"  x: ["+xStr+"],\n" +
													"  y: ["+yStr+"],\n" +
													"  z: ["+pulse.read("Samples")+"],\n" +
													"  marker: {\n" +
													"	   size: 2,\n" +
													"	   line: {\n" +
													"	     width: 0\n" +
													"    },\n" +
													"    opacity: 0.5\n" +
													"  },\n" +
													"  type: 'scatter3d'\n" +
													"},\n" ;
				}
			}
  		res.setBody(
  			plotlyTemplate
  			.replace( "plotlyDiv", "Latest pulse at: "+latestPulseTime+"<button onClick=\"window.open(encodeURI('data:text/csv;charset=utf-8,"+latestPulse+"'))\">Download CSV</button><br><br><div id='plot_div'></div>" )
  			.replace( "plotSize", "autosize:false, width:1200, height:720," )
				.replace( "latestPulseTime", latestPulseTime )
				.replace( "latestPulse", latestPulse )
				.replace( "plotCode",
					"Plotly.newPlot( 'plot_div', [ \n" +
					plotlyData +
					" ], layout );\n"
				).toString()
  		);

  	}
  	
  }

}
