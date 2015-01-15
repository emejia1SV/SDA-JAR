package sv.avantia.depurador.agregadores.hilo;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.w3c.dom.Document;

//import com.cladonia.xml.webservice.soap.SOAPClient;

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

/**
 * Conexion con servicios web montados en una ruta de seguridad HTTPS
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
public class ConsultarHTTPS extends Consultar {

	//private Document 		response 	= null;
	//private SOAPClient 		client 		= null;
	//private URL 			url 		= null;
	
	/**
	 * Metodo que nos carga un certificado digital
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link Void}
	 * */
	static public void doTrustToCertificates() throws Exception 
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
					logger.warn("Warning: URL host '" + urlHostName	+ "' is different to SSLSession host '"	+ session.getPeerHost() + "'.");
				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	
	/**
	 * Invoke a SOAP call passing in an operation instance and attachments
	 * 
	 * @param metodo
	 *            {@link Metodos} The selected operation
	 * @param timeOutMillisecond
	 *            tiempo estimado para timeout excepcion
	 * 
	 * @return The response SOAP Envelope as a String
	 */
	public Document invoke(Metodos metodo, int timeOutMillisecond, String movil)
	{
		logger.debug("init https");
		logger.debug(metodo.getInputMessageText());

		URL url = null;
		try {
			url = new URL(metodo.getEndPoint());
			//verificamos si donde esta corriendo tiene acceso o no a la ruta del agregador
			if (!ping(url.getHost())) {
				logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.EL_SERVIDOR_DONDE_ESTA_EJECUTANDOSE_NO_TIENE_ACCESO_AL_AGREGADOR.getDescripcion()+ " " + metodo.getEndPoint());
				return xmlErrorSDA(ErroresSDA.EL_SERVIDOR_DONDE_ESTA_EJECUTANDOSE_NO_TIENE_ACCESO_AL_AGREGADOR);
			}
		} catch (MalformedURLException e1) {
			logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO.getDescripcion()+ " " + metodo.getEndPoint());
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO);
		}
		
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
			SOAPMessage msg = messageFactory.createMessage(
					new MimeHeaders(),
					new ByteArrayInputStream(metodo.getInputMessageText().getBytes(Charset.forName("UTF-8"))));

			logger.debug("init https cert");
			// Trust to certificates
			doTrustToCertificates();

			logger.debug("init https msg to send");
			System.out.println("in");
			msg.writeTo(System.out);
			
			logger.debug("init https send" + metodo.getEndPoint());
			// SOAPMessage rp = conn.call(msg, urlval);
			SOAPMessage rp = sendMessage(msg, url);

			// View the output
			logger.debug("Soap response https received... Done");
			rp.writeTo(System.out);
			
			logger.debug("init https return ");
			return toDocument(rp);
		} 
		catch (Exception e) 
		{
			logger.error(e.getMessage());
			return xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_CON_SEGURIDAD);
		}
	}
	
	static public SOAPMessage sendMessage(SOAPMessage message, URL url)
			throws MalformedURLException, SOAPException 
	{
		SOAPMessage result = null;
		if (message != null) 
		{
			SOAPConnectionFactory scf = SOAPConnectionFactory.newInstance();
			SOAPConnection connection = null;
			long time = System.currentTimeMillis();
			try 
			{
				connection = scf.createConnection(); // point-to-point connection
				result = connection.call(message, url);
			} 
			finally 
			{
				if (connection != null) 
				{
					connection.close();
				}
			}
			logger.debug("Respuesta https en " + (System.currentTimeMillis() - time));
		}
		return result;
	}
}
