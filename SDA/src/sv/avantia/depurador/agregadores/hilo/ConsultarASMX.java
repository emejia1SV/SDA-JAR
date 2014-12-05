package sv.avantia.depurador.agregadores.hilo;

import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.w3c.dom.Document;

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

/**
 * cliente ASMX para las implementaciones con servicios web montados en windows
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
public class ConsultarASMX extends Consultar {

	private HttpClient 		httpClient 	= null;
	private HttpPost 		postRequest = null;
	private HttpResponse 	response 	= null;
	private StringEntity 	input 		= null;
	private Document 		salidaError = null;
	/**
	 * Metodo para la invocacion de los servicios ASMX
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param operation un bjeto {@link Metodos}
	 * @return {@link Document}
	 * */
	@SuppressWarnings("deprecation")
	public Document invoke(Metodos metodo, long timeOutMillisecond)  
	{	
		try
		{	
			//generamos el cliente
			httpClient = new org.apache.http.impl.client.DefaultHttpClient();
			
			//inyectamos la ubicacion del servico WEB
			postRequest = new HttpPost(metodo.getEndPoint());

			try 
			{
				//inyectamos el mensaje request
				input = new StringEntity(metodo.getInputMessageText());
			} 
			catch (UnsupportedEncodingException e) 
			{
				logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_INYECTAR_MENSAJE_ENVIO_POR_ASMX.getDescripcion() + " " + e.getMessage(), e);
				return xmlErrorSDA(ErroresSDA.ERROR_AL_INYECTAR_MENSAJE_ENVIO_POR_ASMX);
			}
			
			//le indicamos el tipo del contenido del mensaje de envio
			input.setContentType(metodo.getContentType());
			
			//ingresamos el mensaje a la comunicacion.
			postRequest.setEntity(input);
	        
			Thread taskInvoke;
			try 
			{
				Runnable run = new Runnable() 
				{
					public void run() 
					{
						try 
						{
							//invocamos el metodo web del Servicio
							response = httpClient.execute(postRequest);
						} 
						catch (Exception e) 
						{
							logger.error(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX.getDescripcion() + "" + e.getMessage(), e);
				        	salidaError = xmlErrorSDA(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX);
						}
					}
				};
				taskInvoke = new Thread(run, "ServicioWeb"+metodo.getMetodo());
				taskInvoke.start();

				if(salidaError != null)
		        	return salidaError;
				
				int m_seconds = 1;
				int contSeconds = 0;
				while (true) 
				{
					Thread.sleep(m_seconds * 1000);
					contSeconds += m_seconds;
					if (response != null) 
						break;
					
					if(salidaError != null)
			        	break;
					
					if (contSeconds >= timeOutMillisecond) 
					{
						taskInvoke.interrupt();
						logger.error(ErroresSDA.ERROR_TIMEUP_EXCEPTION.getDescripcion());
						salidaError = xmlErrorSDA(ErroresSDA.ERROR_TIMEUP_EXCEPTION);
						break;
					}
				}
				if (taskInvoke.isAlive()) 
					taskInvoke.interrupt();				
			} 
			catch (Exception e) 
			{
				logger.error(ErroresSDA.ERROR_EL_CONSULTAR_TIMEUP_EN_EL_METODO_A_TRAVES_DE_ASMX.getDescripcion(), e);
				return xmlErrorSDA(ErroresSDA.ERROR_EL_CONSULTAR_TIMEUP_EN_EL_METODO_A_TRAVES_DE_ASMX);
			}
			
	        if(salidaError != null)
	        	return salidaError;
			
	        //lectura de la respuesta obtenida
	        try 
	        {
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				if (response.getStatusLine().getStatusCode() != 200) 
				{
					logger.error(getStringFromDocument(factory.newDocumentBuilder().parse(response.getEntity().getContent())));
					logger.error(ErroresSDA.ERROR_OBTENIDO_EN_LA_RESPUESTA_DEL_METODO_POR_ASMX_CODE_ERROR.getDescripcion()+ ": " + response.getStatusLine().getStatusCode());
					return xmlErrorSDA(ErroresSDA.ERROR_OBTENIDO_EN_LA_RESPUESTA_DEL_METODO_POR_ASMX_CODE_ERROR);
				}
				
				// Obtener informaci�n de la respuesta
				return factory.newDocumentBuilder().parse(response.getEntity().getContent());	
			} 
	        catch (Exception e) 
	        {
				logger.error(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX + " " + e.getMessage() , e);
				return xmlErrorSDA(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX);
			}
		}
		finally
		{
			// Cierre de la conexi�n
			try {
				if (httpClient != null) httpClient.getConnectionManager().shutdown();
			} 
			catch (Exception e) 
			{
				logger.warn(getAgregador().getNombre_agregador() + " Verificar porque no se pudo dar shutdown a la conexion httpClient " + e.getMessage());
			}
		}
	}
}