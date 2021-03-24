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
    
    Server testPost 				= new ServerHTTP( c, 9000, "Web Interface" );
    //Server displayLast		 	= new ServerHTTP( c, 9001, "display last pulse" );
    //Server display100 			= new ServerHTTP( c, 9002, "display last 100 pulses" );

    Server pulsePost 				= new ServerHTTP( c, 49157, "Binary Data" );
    Server pulsePostTest		= new ServerHTTP( c, 49154, "Hex-String Data" );

		

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
		 .input( "pulses", "Samples", arrayDoubleJoin( pd.samples() ) )
		 .input( "pulses", "Amplitude", arrayDoubleJoin( pd.amplitude() ) )
		 .execute( true ); // write flag
		//System.out.println( q );
	}
	
	public List<TableRow> pulseList ( int sessionId, String queryData ) {
		Query q = pulseDatabase.query( sessionId );
		q.execute( queryData );
		//System.out.println( q.rows() );
		List<TableRow> pulses = new ArrayList<>();
		for (TableRow tr : q.rows( q.db().table("pulses"), "Server Timestamp", "" )) { // use "Server Timestamp" as the filter
			pulses.add( tr );
		}
		return pulses;
	}
	
	private String notNull ( String str ) {
		return ( str != null ? str : "" );
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
  		res.setBody( "Received hex-string data:\n"+req.data()+"\n"+pd );
  		
		////////////////////////// 49157
  	} else if ( req.socket().getLocalPort() == 49157 ) {
  		PulseData pd = new PulseData( req.data().getBytes(), 16 );
  		addPulse( req.sessionId(), pd );
  		res.setMIME( "text/plain" );
  		res.setBody( "Received binary data:\n\n"+pd );
  		  		
		////////////////////////// dummy data
  	} else if ( req.path().toLowerCase().equals("/dummy") ) {
  		PulseData pd = dpd.spawn();
  		addPulse( req.sessionId(), pd );
  		res.setMIME( "text/plain" );
  		res.setBody( "Dummy data:\n"+pd );
  		
		////////////////////////// filter (2d)
  	} else if ( req.path().toLowerCase().equals("/filter") ) {
			Query q = pulseDatabase
				.query( req.sessionId()	)
				.output( "pulses", "Samples" ) // output (not used as filter)
				.output( "pulses", "Amplitude" ) // output (not used as filter)
				.execute( req.data() ); // parses HTTP data adds inputs (filters)
			//System.out.println( q );
			String plotlyHeader =
										"<form>\n"+
										"<table>\n"+
										"<tr>\n"+
										"	<th>Received Time</th>\n"+
										"	<th>Capture ID</th>\n"+
										"	<th>Total Samples</th>\n"+
										"	<th>Pulse Duration</th>\n"+
										"	<th rowspan=2>Chart</th>\n"+
										"</tr>\n"+
										"<tr>\n"+
										"	<td>"+
										"		<input type=\"button\" value=\"Today\" onclick=\""+
													"this.form.elements['pulses.Server Timestamp'].value='"+LocalDateTime.now().toString().substring(0,10)+"';"+
													"this.form.elements['pulses.Capture ID'].value='';"+
													"this.form.elements['pulses.Server Count'].value='';"+
													"this.form.elements['pulses.duration'].value='';"+
													"this.form.submit();"+
												"\">\n"+
										"		<input type=\"text\" name=\"pulses.Server Timestamp\" value=\""+notNull(q.input("pulses","Server Timestamp"))+"\" placeholder=\"Filter\" onblur=\"this.form.submit();\">\n"+
										"	</td>\n"+
										"	<td><input type=\"text\" name=\"pulses.Capture ID\" value=\""+notNull(q.input("pulses","Capture ID"))+"\" placeholder=\"Filter\" onblur=\"this.form.submit();\" size=5></td>\n"+
										"	<td><input type=\"text\" name=\"pulses.Server Count\" value=\""+notNull(q.input("pulses","Server Count"))+"\" placeholder=\"Filter\" onblur=\"this.form.submit();\" size=5></td>\n"+
										"	<td><input type=\"text\" name=\"pulses.duration\" value=\""+notNull(q.input("pulses","duration"))+"\" placeholder=\"Filter\" onblur=\"this.form.submit();\" size=5></td>\n"+
										"</tr>\n";
			String plotlyFooter =
										"</table>\n"+
										"</form>\n";
			String plotlyRows = "";
			String plotCode = "";
			int i=0;
//			for (int i=qRowKeys.length-1; i>=0; i--) {
			for (String qRow : q.rows().keys()) {
				String divId = "plot"+(i++);
				String thisPulseTime = q.rows().read(qRow, "pulses", "Server Timestamp");
				plotlyRows =
										"<tr>"+
										"	<td><a href=\"csv?pulses.Server+Timestamp="+thisPulseTime+"\">"+thisPulseTime+"</a></td>\n"+
										"	<td>"+q.rows().read(qRow, "pulses", "Capture ID")+"</td>\n"+
										//"	<td>"+result.read(qRow, "pulses", "FPGA Count")+"</td>\n"+
										"	<td>"+q.rows().read(qRow, "pulses", "Server Count")+"</td>\n"+
										"	<td>"+q.rows().read(qRow, "pulses", "duration")+"</td>\n"+
										"	<td><div id='"+divId+"'></div></td>\n"+
										"</tr>\n"+
										"\n"+
										plotlyRows;
				plotCode +=
										"Plotly.newPlot( '"+divId+"', [ \n"+
										"	{type:'scatter', fill:'tozeroy', marker:{opacity:'0.2', color:'#00ffff'}, y:["+q.rows().read(qRow, "pulses", "Amplitude")+"]},\n"+
										"	{type:'scatter', marker:{opacity:'0.2', color:'#0000ff'}, y:["+q.rows().read(qRow, "pulses", "Samples")+"]},\n"+
										" ], layout );\n";
			}
  		res.setBody(
  			plotlyTemplate
  			.replace( "plotSize", "autosize:false, width:500, height:100," )
  			.replace( "plotlyDiv", plotlyHeader+plotlyRows+plotlyFooter )
				.replace( "plotCode", plotCode
				).toString()
  		);
  		
  	////////////////////////// CSV
  	} else if ( req.path().toLowerCase().equals("/csv") ) {
			Query q = pulseDatabase
				.query( req.sessionId()	)
				.output( "pulses", "Server Timestamp" )
				.output( "pulses", "Samples" )
				.execute( req.data() ); // parses HTTP data adds inputs (filters)
			System.out.println( q );
			String csvStr = "";
			for (String qRow : q.rows().keys()) {
				csvStr += q.rows().read(qRow, "pulses", "Server Timestamp")+","+q.rows().read(qRow, "pulses", "Samples")+"\n";
			}
			res.setBody( csvStr );
			res.setMIME( "text/csv" );
			
		////////////////////////// latest
  	} else if ( req.path().toLowerCase().equals("/latest") ) {
   		List<TableRow> pulses = pulseList( req.sessionId(), "pulses.Server+Timestamp.Last=" );  		
			String latestPulseTime = pulses.get(0).read("Server Timestamp");
  		res.setBody(
  			plotlyTemplate
  			.replace( "plotlyDiv", "Latest pulse at <a href=\"csv?pulses.Server+Timestamp="+latestPulseTime+"\">"+latestPulseTime+"<br><br><div id='plot_div'></div>" )
  			.replace( "plotSize", "autosize:false, width:1200, height:720," )
				//.replace( "latestPulseTime", latestPulseTime )
				//.replace( "latestPulse", latestPulse )
				.replace( "plotCode",
					"Plotly.newPlot( 'plot_div', ["+
					"{type:'scatter', fill:'tozeroy', marker:{opacity:'0.2', color:'#00ffff'}, y:["+
					pulses.get(0).read("Amplitude") +
					"]}," +
					"{type:'scatter', marker:{opacity:'0.2', color:'#0000ff'}, y:["+
					pulses.get(0).read("Samples") +
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
