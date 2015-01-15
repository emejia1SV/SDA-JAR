package sv.avantia.depurador.agregadores.hilo;

import java.net.MalformedURLException;
import java.net.URL;

import org.w3c.dom.Document;

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

import com.cladonia.xml.webservice.soap.SOAPClient;
import com.cladonia.xml.webservice.soap.SOAPException;

/**
 * Metodo para invocar los metodos normales que no sean https ni que los endpoint terminen conn asmx
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
public class ConsultarHTTP extends Consultar {
	
	private Document response = null;
	private SOAPClient client = null;
	private URL url = null;
	
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
		
		
		logger.debug("mensaje en texto");
		logger.debug(metodo.getInputMessageText());
		Document docRequest = getdocumentFromString(metodo.getInputMessageText());;
		
		if(docRequest==null)
		{
			logger.error(ErroresSDA.ERROR_PASANDO_DE_CADENA_TEXTO_A_DOCUMENT.getDescripcion());
			return xmlErrorSDA(ErroresSDA.ERROR_PASANDO_DE_CADENA_TEXTO_A_DOCUMENT);
		}
		
		logger.debug("documento inputtext");//------------------
		logger.debug(docRequest.getDocumentElement().toString());//------------------
		
		logger.debug("timeOutMillisecond " + timeOutMillisecond);//------------------
		
		// create the saaj based soap client
		try 
		{
			client = new SOAPClient(docRequest);
		} catch (SOAPException e) {
			logger.error(ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT.getDescripcion() + " " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT);
		}

		logger.debug("SoapActionURI: " + metodo.getSoapActionURI());//------------------
		// set the SOAPAction
		client.setSOAPAction(metodo.getSoapActionURI());
		
		
		
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
					} catch (Exception ex) 
					{
						logger.error(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD.getDescripcion() + " " + url + " " + ex.getMessage(), ex);
						response = xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD);
					}
				}
			};

			taskInvoke = new Thread(run, "WSHTTP-"+getAgregador().getNombre_agregador()+movil);
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
					logger.error(ErroresSDA.ERROR_TIMEUP_EXCEPTION.getDescripcion());
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