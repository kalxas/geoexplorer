
package it.geosolutions.xmlJsonTranslate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter; 
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.HashMap;
import java.net.URLDecoder;

/**
 * Servlet implementation class FileUploader
 */
public class FileDownloader extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private final static String PROPERTY_FILE_PARAM = "app.properties";
	private final static Logger LOGGER = Logger.getLogger(FileDownloader.class.getSimpleName());
	private Properties properties = new Properties();
		private String tempDirectory;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileDownloader() {
        super();
    }
    
    public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		String appPropertyFile = getServletContext().getInitParameter(PROPERTY_FILE_PARAM); 
		InputStream inputStream = getServletContext().getResourceAsStream(appPropertyFile);	
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.SEVERE)){
				LOGGER.log(Level.SEVERE, "Error encountered while processing properties file", e);
			}
		} 	finally {
				try {
					if (inputStream != null)
						inputStream.close();
				} catch (IOException e) {
					if (LOGGER.isLoggable(Level.SEVERE))
						LOGGER.log(Level.SEVERE,
								"Error building the proxy configuration ", e);
					throw new ServletException(e.getMessage());
				}
			}
		// get the file name for the temporary directory
		String temp = properties.getProperty("temp");
		
		// if it does not exists create the file
		tempDirectory = temp;
		File tempDir = new File(temp);
		if (!tempDir.exists()){
			if(!tempDir.mkdir()){
				LOGGER.log(Level.SEVERE, "Unable to create temporary directory " + tempDir);
				throw new ServletException("Unable to create temporary directory " + tempDir);
			}
				
		}
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		// get parameter name
		String code = request.getParameter("code");
		String filename = request.getParameter("filename");
		
		if (code != null){
			
			File file = null;
			BufferedReader br = null;
			PrintWriter writer = null;
			try{
				// set reponse headers
				response.setContentType("application/force-download");
				response.setHeader("Content-Disposition","attachment; filename=\"" + filename + "\"");
				
				// get file
				file = new File(tempDirectory + "/" + code);
				br = new BufferedReader( new FileReader( file ));
			    writer = response.getWriter();
			    String line = null;
			    while( (line=br.readLine()) != null ){
			    	writer.println( line );
			    }
			    // delete file
			    file.delete();
				
			} catch(IOException ex){
				if (LOGGER.isLoggable(Level.SEVERE)){
					LOGGER.log(Level.SEVERE,
								"Error encountered while downloading file");
				}
				response.setContentType("text/html");
				writeResponse( response, "{ \"success\":false, \"errorMessage\":\""+ ex.getLocalizedMessage() +"\"}" );				
			} finally {
				br.close();
				writer.close();
			}

		} else {
			if (LOGGER.isLoggable(Level.SEVERE)){
				LOGGER.log(Level.SEVERE,
							"malformed request: code param is required");
			}
			response.setContentType("text/html");
			writeResponse( response, "{ \"success\":false, \"errorMessage\":\"malformed request: code param is required\"}" );
		}

	}



	/**
	 * read and save on file the content of post request
	 * return a json where the name of the file is returned
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
			BufferedReader in = null;
			BufferedWriter out = null;
			try{
				// read the content data for this request
				in = new BufferedReader(
										new InputStreamReader( 
												request.getInputStream()) );
				// create a file with a random name

				String uuid = UUID.randomUUID().toString();
				out = new BufferedWriter(new FileWriter( tempDirectory + "/" + uuid ));

				StringBuffer sb = new StringBuffer();
				String line = null;
				while ( (line=in.readLine()) != null ){
					sb.append( line );
				}
				
				String input = URLDecoder.decode(sb.toString());
				Map<String, String> fields = getFields(input);
				String content = fields.get("content");

				out.write( content );

			    response.setContentType("text/html");	        
				writeResponse(response, "{ \"success\":true, \"result\":{ \"code\":\""+ uuid +"\"}}");
			} catch (IOException ex) {
				if (LOGGER.isLoggable(Level.SEVERE))
					LOGGER.log(Level.SEVERE,
								"Error encountered while uploading file");

				response.setContentType("text/html");
				writeResponse( response, "{ \"success\":false, \"errorMessage\":\""+ ex.getLocalizedMessage() +"\"}" );


			} finally {
				if ( in != null ){
					in.close();
				}
				if ( out != null ){
					out.close();
				}
			}
	
	}
	
	
	private Map<String, String> getFields(String input){
		String[] params = input.split("&");
		HashMap<String, String> fields = new HashMap<String, String>();
		for(String param: params){
			String value = param.substring(param.indexOf("=")+1, param.length());
			String key = param.substring(0, param.indexOf("="));
			fields.put(key, value);
		}
		return fields;
	}
	
	private void writeResponse(HttpServletResponse response, String text)
			throws IOException {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			writer.write( text );
		} catch (IOException e) {
			if (LOGGER.isLoggable(Level.SEVERE))
				LOGGER.log(Level.SEVERE, e.getMessage());
		} finally {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}	

}
