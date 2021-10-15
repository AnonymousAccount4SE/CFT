package rf.configtool.main.runtime.lib.web;

import java.io.*;
import java.net.Socket;
import java.util.*;

import rf.configtool.main.Version;

public 	class ClientMain implements Runnable {
	private ObjServer objServer;
	
	private boolean completed=false;
	private Socket sock;
	
	public synchronized boolean isCompleted() {
		return completed;
	}
	private synchronized void setCompleted() {
		this.completed=true;
	}
	
	public ClientMain (Socket sock, ObjServer objServer) {
		this.sock=sock;
		this.objServer=objServer;
	}

	private void sendData (PrintWriter out, BufferedOutputStream outBinary, ResponseData resp) throws Exception {
		out.println("HTTP/1.1 200 OK");
		out.println("Server: CFT " + Version.getVersion());
		out.println("Date: " + new Date());
		out.println("Content-type: " + resp.getContentType());
		out.println("Content-length: " + resp.getData().length);
		out.println(); 
		out.flush();
		
		outBinary.write(resp.getData());
		outBinary.flush();
		outBinary.flush();
	}
	
	private List<String> getRequestPlusHeaders (InputStream in) throws Exception {
		byte[] buf=new byte[64*1024];
		int pos=0;
		int count=0;
		for (;;) {
			int b=in.read();
			buf[pos++]=(byte) b;
			if (b=='\n') {
				count++;
				if (count==2) break;
			} else if (b=='\r') {
				// no change to count
			} else {
				count=0;
			}
		}
		List<String> result=new ArrayList<String>();
		BufferedReader br=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, pos)));
		for (;;) {
			String line=br.readLine();
			if (line==null) break;
			result.add(line);
		}
		return result;
	}
	
	
	
	
	public void run () {
		InputStream in = null;
		PrintWriter out = null; 
		BufferedOutputStream outBinary = null;
		
		try {
			sock.setSoTimeout(10000);
			in = sock.getInputStream();
			out = new PrintWriter(sock.getOutputStream()); 
				// remember to flush out before using outBinary
			outBinary = new BufferedOutputStream(sock.getOutputStream());
			
			List<String> lines=getRequestPlusHeaders(in);

			StringTokenizer st = new StringTokenizer(lines.get(0)," ",false);
			String method = st.nextToken().toUpperCase(); // we get the HTTP method of the client
			String url = st.nextToken().toLowerCase();
			
			Map<String,String> urlParams = null;
			if (url.contains("?")) {
				int pos=url.indexOf('?');
				urlParams=splitAndDecodeUrlFields(url.substring(pos+1),"UTF-8");
				url=url.substring(0,pos);
			}
			
			Map<String,String> headers = new HashMap<String,String>();
			for (int i=1; i<lines.size(); i++) {
				String line=lines.get(i);
				if (line==null) break;
				
				if (line.trim().length()==0) break; // end of headers
				int colonPos=line.indexOf(':');
				if (colonPos <= 0) continue;
				
				String name=line.substring(0,colonPos).trim();
				String value=line.substring(colonPos+1).trim();
				
				headers.put(name, value);
				//System.out.println("HDR> " + line);
			}
			
			
			byte[] body=null;
			if (headers.containsKey("Content-Length")) {
				int contentLength=Integer.parseInt(headers.get("Content-Length"));
				
				body=new byte[contentLength];
				int bytesRead=in.read(body);
				if (bytesRead < 0) throw new Exception("Got eof from inBinary");
				if (bytesRead != contentLength) throw new Exception("Should do repeat reads on content? (" + bytesRead + " of " + contentLength + ")");
			}
			
			Map<String,String> bodyParams=null;
			if (headers.containsKey("Content-Type") && body != null) {
				if (headers.get("Content-Type").toLowerCase().equals("application/x-www-form-urlencoded")) {
					String a=new String(body,"ISO-8859-1");
					bodyParams=splitAndDecodeUrlFields(a, "UTF-8");
				}
				
			}
			
			ObjRequest request=new ObjRequest(headers, method, url, urlParams, body, bodyParams);
			
			if (method.equals("GET")) {
				try {
					ResponseData resp=objServer.processGETRequest(request);
					//System.out.println("Sending data=" + data.length+" bytes");
					sendData(out, outBinary, resp);
				} finally {
					sock.close();
				}
			} else if (method.equals("POST")) {
				try {
					ResponseData resp=objServer.processPOSTRequest(request);
					//System.out.println("Sending data=" + data.length+" bytes");
					sendData(out, outBinary, resp);
				} finally {
					sock.close();
				}
			} else {
				byte[] data=("<html><body><p>"+(new Date())+" invalid method " + method).getBytes("UTF-8");
				ResponseData resp=new ResponseData("text/html", data);
				
				System.out.println("Sending invalid method message (" + method + ")");
				sendData(out, outBinary, resp);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		setCompleted();
	}
	
	private Map<String,String> splitAndDecodeUrlFields (String s, String charset) throws Exception {
		Map<String,String> result=new HashMap<String,String>();
		StringTokenizer st=new StringTokenizer(s,"&",false);
		while (st.hasMoreTokens()) {
			String assignment=st.nextToken();
			assignment=java.net.URLDecoder.decode(assignment, charset);
			int pos=assignment.indexOf('=');
			if (pos > 0) {
				String name=assignment.substring(0,pos);
				String value=assignment.substring(pos+1);
				result.put(name, value);
			}
		}
		return result;
	}
}