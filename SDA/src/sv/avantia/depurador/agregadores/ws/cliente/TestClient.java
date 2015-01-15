package sv.avantia.depurador.agregadores.ws.cliente;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;

@SuppressWarnings("deprecation")
public class TestClient  {

	/**
	 * Metodo que nos carga un certificado digital
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link Void}
	 * @throws KeyManagementException 
	 * @throws NoSuchAlgorithmException 
	 * */
	static public void doTrustToCertificates() throws KeyManagementException, NoSuchAlgorithmException  
	{
		TrustManager[] trustAllCerts = new TrustManager[] 
		{ 
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() 
					{
						return null;
					}
		
					public void checkServerTrusted(X509Certificate[] certs,	String authType) 
							throws CertificateException 
					{
						return;
					}
		
					public void checkClientTrusted(X509Certificate[] certs,	String authType) 
							throws CertificateException 
					{
						return;
					}
				} 
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hv = new HostnameVerifier() 
		{
			public boolean verify(String urlHostName, SSLSession session) 
			{
				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) 
				{
					System.out.println("Warning: URL host '" + urlHostName	+ "' is different to SSLSession host '"	+ session.getPeerHost() + "'.");
				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	
	//private boolean			iniciarProceso 	= false;
	/**
	 * Metodo para la invocacion de los servicios ASMX
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param operation un bjeto 
	 * @return {@link Document}
	 * */
	@SuppressWarnings("resource")
	public static Document invoke( int timeOutMillisecond)  
	{
		long init = System.currentTimeMillis();
		
		 HttpClient 		httpClient 		= null;
		 HttpPost 		postRequest 	= null;
		 HttpResponse 	response 		= null;
		 StringEntity 	input 			= null;
		 Document 		salidaError 	= null;
		
		try
		{	
			System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 1");
			
			doTrustToCertificates();
			// set the connection timeout value to 30 seconds (30000 milliseconds)
		    final HttpParams httpParams = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParams, timeOutMillisecond);
		    
			//generamos el cliente
			httpClient = new org.apache.http.impl.client.DefaultHttpClient(httpParams);
			
			//inyectamos la ubicacion del servico WEB
			postRequest = new HttpPost("https://hub.americamovil.com/sag/services/blackgrayService");

			System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 2");
			
			try 
			{
				//inyectamos el mensaje request
				input = new StringEntity("<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:loc=\"http://www.csapi.org/schema/parlayx/blackgray/v1_0/local\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><wsse:Username>PA00000737</wsse:Username><wsse:Password Type=\"...#PasswordDigest\">QoENdQL6Hhtan6Ixlztx0Tea98c=</wsse:Password><wsse:Nonce>46bf15be-e5e2-4ce8-8224-d8c010b56179</wsse:Nonce><wsse:Created>2014-11-06T17:07:50Z</wsse:Created></wsse:UsernameToken></wsse:Security><tns:RequestSOAPHeader xmlns:tns=\"http://www.huawei.com.cn/schema/common/v2_1\"><tns:AppId>35000001000001</tns:AppId><tns:TransId>2014011716010012345</tns:TransId><tns:OA>50251263698</tns:OA><tns:FA>50251263698</tns:FA></tns:RequestSOAPHeader></soapenv:Header><soapenv:Body><loc:deleteGrayList><loc:version>1.0</loc:version><loc:grayList><grayee><msisdn>50251263698</msisdn></grayee></loc:grayList></loc:deleteGrayList></soapenv:Body></soapenv:Envelope>");
			} 
			catch (UnsupportedEncodingException e) 
			{
				e.printStackTrace();
			}
			
			System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 3");
			
			//le indicamos el tipo del contenido del mensaje de envio
			input.setContentType("text/xml;charset=UTF-8");
			
			//ingresamos el mensaje a la comunicacion.
			postRequest.setEntity(input);
			
			System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 4");
			
			//invocamos el metodo web del Servicio
			response = httpClient.execute(postRequest);
			
			System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 5");
			
	        //lectura de la respuesta obtenida
	        try 
	        {
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				if (response.getStatusLine().getStatusCode() != 200) 
				{
					return null;
				}
				
				// Obtener información de la respuesta
				return factory.newDocumentBuilder().parse(response.getEntity().getContent());	
			} 
	        catch (Exception e) 
	        {
				e.printStackTrace();
			}
	        
	        System.out.println( (System.currentTimeMillis() - init)  + " Mili Segundos 6");
	    
		} catch (ConnectTimeoutException e1) {
			// TODO Auto-generated catch block
			System.err.println("vaya vaya");
			e1.printStackTrace();
		
		} catch (ClientProtocolException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (KeyManagementException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		finally
		{
			// Cierre de la conexión
			try {
				if (httpClient != null) httpClient.getConnectionManager().shutdown();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		return salidaError;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long init = System.currentTimeMillis();
		System.out.println(getStringFromDocument(invoke(1000)));
		System.out.println("finalizo la depuración Masiva de los numeros en " + (System.currentTimeMillis() - init)  + " Mili Segundos");
	}

	/**
	 * Metodo para la transformación de un {@link Document} a un {@link String}
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param soapMsg
	 *            {@link SOAPMessage}
	 * @return {@link Document}
	 * */
	protected static String getStringFromDocument(Document doc)
	{
	    try
	    {
	       DOMSource domSource = new DOMSource(doc);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory tf = TransformerFactory.newInstance();
	       Transformer transformer = tf.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    }
	    catch(TransformerException ex)
	    {
	    	ex.printStackTrace();
	    }
		return null;
	}
}
