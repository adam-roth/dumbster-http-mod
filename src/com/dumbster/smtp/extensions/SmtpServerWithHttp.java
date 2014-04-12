package com.dumbster.smtp.extensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class SmtpServerWithHttp extends SimpleSmtpServer {
	private static final int DEFAULT_HTTP_PORT = 10082;
	
	private int httpPort;
	private int smptPort;
	private ServerSocket httpSocket;
	
	public SmtpServerWithHttp(int smtpPort, int httpPort) {
		super(smtpPort);
		this.smptPort = smtpPort;
		this.httpPort = httpPort;
	}
	
	public SmtpServerWithHttp() {
		this(DEFAULT_SMTP_PORT, DEFAULT_HTTP_PORT);
	}
	
	@Override
	public void run() {
		//FIXME:  spawn HTTP thread here
		HttpDaemon httpServer = new HttpDaemon();
		httpServer.start();
		
		synchronized(httpServer) {
			try {
				httpServer.wait();
			}
			catch (Exception ignored) {
				//ignored
			}
		}
		
		super.run();
	}
	
	public int getHttpPort() {
		return this.httpPort;
	}
	
	public int getSmtpPort() {
		return this.smptPort;
	}
	
	public String getWebmailAddress() {
		return "http://localhost:" + this.getHttpPort();
	}
	
	/**
	  * Creates an instance of SimpleSmtpServer and starts it. Will listen on the default port.
	  * @return a reference to the SMTP server
	  */
	public static SmtpServerWithHttp start() {
		return start(DEFAULT_SMTP_PORT, DEFAULT_HTTP_PORT);
	}

	/**
	 * Creates an instance of SimpleSmtpServer and starts it.
	 * @param port port number the server should listen to
	 * @return a reference to the SMTP server
	 */
	public static SmtpServerWithHttp start(int smtpPort, int httpPort) {
		SmtpServerWithHttp server = new SmtpServerWithHttp(smtpPort, httpPort);
	    Thread t = new Thread(server);
	    t.start();
	    
	    // Block until the server socket is created
	    synchronized (server) {
	    	try {
	    		server.wait();
	    	} catch (InterruptedException e) {
	    		// Ignore don't care.
	    	}
	    }
	    return server;
	}
	
	/**
	 * For testing, will start an instance of the server and leave it running for 10 minutes.
	 * 
	 * @param args not used
	 */
	public static void main(String[] args) {
		SmtpServerWithHttp server = SmtpServerWithHttp.start();
		
		try {
			Thread.sleep(1000 * 60 * 10);
		}
		catch (Exception ignored) {
			//ignored
		}
		
		server.stop();
	}
	
	private class HttpDaemon extends Thread {
		@Override
		public void run() {
			stopped = false;
		    try {
		    	try {
		    		httpSocket = new ServerSocket(httpPort);
		    		httpSocket.setSoTimeout(TIMEOUT); // Block for maximum of 1.5 seconds
		    	} finally {
		    		synchronized (this) {
		    			// Notify when server socket has been created
		    			notifyAll();
		    		}
		    	}

		    	// Server: loop until stopped
		    	while (!isStopped()) {
		    		// Start server socket and listen for client connections
		    		Socket socket = null;
		    		try {
		    			socket = httpSocket.accept();
		    		} catch (Exception e) {
		    			if (socket != null) {
		    				socket.close();
		    			}
		    			continue; // Non-blocking socket timeout occurred: try accept() again
		    		}

		    		// Get the input and output streams
		    		BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		    		OutputStream out = socket.getOutputStream();

		    		//write response header
		    		writeResponseHeader(out);
		    		
		    		//write response body
		    		String query = parseUrl(input.readLine());
		    		if (query != null) {
			    		String body = computeBody(query);
			    		writeResponse(out, body);
		    		}
		    		
		    		//done, close streams
		    		input.close();
		    		out.flush();
		    		out.close();
		    		socket.close();
		    	}
		    } catch (Exception e) {
		    	/** @todo Should throw an appropriate exception here. */
		    	e.printStackTrace();
		    } finally {
		    	if (httpSocket != null) {
		    		try {
		    			httpSocket.close();
		    		} catch (IOException e) {
		    			e.printStackTrace();
		    		}
		    	}
		    }
		}
		
		private String computeBody(String query) {
			String page = "";
			
			if (query.contains("getMessage")) {
				//display the requested message
				page += "<html>" +
							"<head></head>" +
							"<body>";
				
				int emailId = Integer.parseInt(getParameter(query, "id"));
				synchronized(receivedMail) {
					//FIXME:  linkify url's
					SmtpMessage message = receivedMail.get(emailId);
					page += "Sent By " + message.getHeaderValue("From") + " On:  " + message.getHeaderValue("Date") + "<br />";
					page += "To:  " + message.getHeaderValue("To") + "<br />";
					page += "Reply To:  " + message.getHeaderValue("Reply-To") + "<br /><br />";
					
					page += linkify(message.getBody());
				}
			}
			else {
				//display a page listing the recieved messages
				page += "<html>" +
							"<head></head>" +
							"<body>" +
								"<table>" +
									"<tr>" +
										"<td><b>From</b></td><td><b>Subject</b></td><td><b>Recieved</b></td>" +
									"</tr>";
				int messageId = 0;
				synchronized(receivedMail) {
					for (SmtpMessage message : receivedMail) {
						//Iterator debug = message.getHeaderNames();
						//while(debug.hasNext()) {
						//	String headerName = (String)debug.next();
						//	System.out.println("HEADER:  " + headerName + "=" + fullHeaderValue(message, headerName));
						//}
						String from = fullHeaderValue(message, "From");
						if (from.contains("@")) {
							from = from.substring(0, from.indexOf("@"));
						}
						
						page += "<tr><td><a href='" + getWebmailAddress() + "/getMessage?id=" + messageId + "'>" + from + "</a></td>";
						page += "<td><a href='" + getWebmailAddress() + "/getMessage?id=" + messageId + "'>" + fullHeaderValue(message, "Subject") + "</a></td>";
						page += "<td><a href='" + getWebmailAddress() + "/getMessage?id=" + messageId + "'>" + fullHeaderValue(message, "Date") + "</a></td></tr>";
						messageId++;
					}
				}
				
				page += "</table>";
			}
			
			page += "</body></html>\n";
			return page;
		}
		
		private String fullHeaderValue(SmtpMessage message, String name) {
			String[] headerValues = message.getHeaderValues(name);
			if (headerValues == null) {
				return "";
			}
			
			String valString = "";
			for (String val : headerValues) {
				if (! "".equals(valString)) {
					valString += " ";
				}
				valString += val;
			}
			
			return valString;
		}
		
		private String linkify(String body) {
			body = doLinkReplace(body, "http://");
			body = doLinkReplace(body, "https://");
			
			//fix whitespace
			body = body.replaceAll("\\n", "<br />").replaceAll("\\r", "<br />").replace("\\f", "<br />");
			
			return body;
		}
		
		private String doLinkReplace(String body, String replace) {
			if (! body.contains(replace)) {
				return body;
			}
			String processed = "";
			while (body.contains(replace)) {
				processed += body.substring(0, body.indexOf(replace));
				body = body.substring(body.indexOf(replace));
				
				int linkIndex = 0;
				char next = 'h';
				while (next != ' ' && next != '\t' && next != '\n') {
					linkIndex++;
					next = body.charAt(linkIndex);
				}
				String link = body.substring(0, linkIndex);
				body = body.substring(linkIndex);
				
				processed += "<a target='_blank' href='" + link + "'>" + link + "</a>";
			}
			body = processed + body;
				
			return body;
		}
		
		private String getParameter(String query, String param) {
			String lookFor = param + "=";
			if (! query.contains(lookFor)) {
				return null;
			}
			query = query.substring(query.indexOf(lookFor) + lookFor.length());
			if (query.contains("&")) {
				query = query.substring(0, query.indexOf("&"));
			}
			
			return query.trim();
		}
		
		/**
		 * Computes the desired context-path from the HTTP request.
		 * 
		 * @param requestHeader the first line of the HTTP request, should be something like "GET / HTTP/1.1"
		 * 
		 * @return a string indicating the requested context page, like "/" or "/spring?method=doStuff"
		 */
		private String parseUrl(String requestHeader) {
			if (requestHeader == null || "".equals(requestHeader)) {
				return null;
			}
			requestHeader = requestHeader.substring(requestHeader.indexOf("/"));
			return requestHeader.substring(0, requestHeader.indexOf("HTTP"));
		}
		
		private void writeResponse(OutputStream response, String message) throws IOException {
			response.write("Content-Type: text/html\n".getBytes());
			response.write(("Content-Length: text/html" + message.length() + "\n").getBytes());
			response.write("\n".getBytes());
			response.write(message.getBytes());
		}
		
		private void writeResponseHeader(OutputStream response) throws IOException {
			DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss zzz");
    		
			response.write("HTTP/1.0 200 OK\n".getBytes());
			response.write("Server:  dumbster-http-daemon/1.0-alpha\n".getBytes());
			response.write(("Last-Modified:  " + format.format(new Date()) + "\n").getBytes());
		}
	}
}
