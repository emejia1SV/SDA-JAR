package sv.avantia.depurador.agregadores.hilo;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.w3c.dom.Document;

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

import com.cladonia.xml.webservice.soap.SOAPClient;

/**
 * Conexion con servicios web montados en una ruta de seguridad HTTPS
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
public class ConsultarHTTPS extends Consultar {

	private Document 		response 	= null;
	private SOAPClient 		client 		= null;
	private URL 			url 		= null;
	
	/**
	 * Metodo que nos carga un certificado digital
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link Void}
	 * */
	static private void doTrustToCertificates() 
			throws Exception 
	{
		// Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] 
				{ 
				new X509TrustManager() 
				{
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
	public Document invoke(Metodos metodo, long timeOutMillisecond)
	{
		try 
		{
			// Trust to certificates
			doTrustToCertificates();
		} 
		catch (Exception e) 
		{
			logger.error(ErroresSDA.ERROR_AL_VERIFICAR_LOS_CERTIFICADOS_DE_SEGURIDAD.getDescripcion() + " " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_VERIFICAR_LOS_CERTIFICADOS_DE_SEGURIDAD);
		}
		
		Document docRequest = getdocumentFromString(metodo.getInputMessageText());;

		if(docRequest==null)
		{
			logger.error(ErroresSDA.ERROR_PASANDO_DE_CADENA_TEXTO_A_DOCUMENT.getDescripcion());
			return xmlErrorSDA(ErroresSDA.ERROR_PASANDO_DE_CADENA_TEXTO_A_DOCUMENT);
		}
		// create the saaj based soap client
		try 
		{
			client = new SOAPClient(docRequest);
		} 
		catch (com.cladonia.xml.webservice.soap.SOAPException e) 
		{
			logger.error(ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT.getDescripcion() + " " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT);
		}

		// set the SOAPAction
		client.setSOAPAction(metodo.getSoapActionURI());

		// get the url
		try 
		{
			url = new URL(metodo.getEndPoint());
		} 
		catch (MalformedURLException e) 
		{
			logger.error(ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO.getDescripcion() + " " + metodo.getEndPoint() + " - " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO);
		}
		
		Thread taskInvoke;
		try 
		{
			Runnable run = new Runnable() 
			{
				public void run() 
				{
					try 
					{
						// Tratar respuesta del servidor
						response = client.send(url);
					} 
					catch (Exception ex)
					{
						logger.error(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD.getDescripcion() + " " + url + " " + ex.getMessage(), ex);
						response = xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD);
					}
				}

			};

			taskInvoke = new Thread(run, "AInvocacionWebService");
			taskInvoke.start();

			int m_seconds = 1;
			int contSeconds = 0;
			while (true) 
			{
				Thread.sleep(m_seconds * 1000);
				contSeconds += m_seconds;
				if (response != null)
					break;
				
				if (contSeconds >= timeOutMillisecond) 
				{
					taskInvoke.interrupt();
					logger.error(ErroresSDA.ERROR_TIMEUP_EXCEPTION);
					response = xmlErrorSDA(ErroresSDA.ERROR_TIMEUP_EXCEPTION);
				}
			}
			
			if (taskInvoke.isAlive())
				taskInvoke.interrupt();
			
			return response;
		} 
		catch (Exception e) 
		{
			logger.error(ErroresSDA.ERROR_AL_CONSULTAR_TIMEUP_EN_EL_METODO_SIN_SEGURIDAD.getDescripcion(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CONSULTAR_TIMEUP_EN_EL_METODO_SIN_SEGURIDAD);
		}
	}
}
