package sv.avantia.depurador.agregadores.hilo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

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

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

/**
 * cliente ASMX para las implementaciones con servicios web montados en windows
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
@SuppressWarnings("deprecation")
public class ConsultarASMX extends Consultar {

	private HttpClient 		httpClient 	= null;
	private HttpPost 		postRequest = null;
	private HttpResponse 	response 	= null;
	private StringEntity 	input 		= null;
	//private Document 		salidaError = null;
	/**
	 * Metodo para la invocacion de los servicios ASMX
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param operation un bjeto {@link Metodos}
	 * @return {@link Document}
	 * */
	public Document invoke(Metodos metodo, int timeOutMillisecond, String movil)  
	{	
		try
		{	
			try {
				URL url = new URL(metodo.getEndPoint());
				//verificamos si donde esta corriendo tiene acceso o no a la ruta del agregador
				if (!ping(url.getHost())) {
					logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.EL_SERVIDOR_DONDE_ESTA_EJECUTANDOSE_NO_TIENE_ACCESO_AL_AGREGADOR.getDescripcion()+ " " + metodo.getEndPoint());
					return xmlErrorSDA(ErroresSDA.EL_SERVIDOR_DONDE_ESTA_EJECUTANDOSE_NO_TIENE_ACCESO_AL_AGREGADOR);
				}
				
				url = null;
			} catch (MalformedURLException e1) {
				logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO.getDescripcion()+ " " + metodo.getEndPoint());
				return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO);
			}
			
			// set the connection timeout value to 30 seconds (30000 milliseconds)
		    final HttpParams httpParams = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParams, timeOutMillisecond);
		    
			//generamos el cliente
			httpClient = new org.apache.http.impl.client.DefaultHttpClient(httpParams);
			
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
	        
			//invocamos el metodo web del Servicio
			response = httpClient.execute(postRequest);
			
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
				
				// Obtener información de la respuesta
				return factory.newDocumentBuilder().parse(response.getEntity().getContent());	
			} 
	        catch (Exception e) 
	        {
				logger.error(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX + " " + e.getMessage() , e);
				return xmlErrorSDA(ErroresSDA.ERROR_AL_LEER_LA_RESPUESTA_OBTENIDA_DEL_METODO_POR_ASMX);
			}
	        
		} catch (ConnectTimeoutException e) {
			logger.error(ErroresSDA.ERROR_TIMEUP_EXCEPTION + " " + e.getMessage() , e);
			return xmlErrorSDA(ErroresSDA.ERROR_TIMEUP_EXCEPTION);
		} catch (ClientProtocolException e) {
			logger.error(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_A_TRAVES_DE_ASMX+ " " + e.getMessage() , e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_A_TRAVES_DE_ASMX);
		} catch (IOException e) {
			logger.error(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_A_TRAVES_DE_ASMX + " " + e.getMessage() , e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_A_TRAVES_DE_ASMX);
		}
		finally
		{
			// Cierre de la conexión
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