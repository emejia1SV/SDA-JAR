package sv.avantia.depurador.agregadores.hilo;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import sv.avantia.depurador.agregadores.entidades.LogDepuracion;
import sv.avantia.depurador.agregadores.jdbc.BdEjecucion;

public class DepuracionMasiva 
{
	/**
	 * La constante que se enviara como estado de la tansaccion fallida
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private static String ESTADO_ERROR = "ERROR";
	
	/**
	 * Objeto respuesta
	 * */
	private List<LogDepuracion> respuestas;
	
	/**
	 * Obtener el appender para la impresión en un archivo de LOG
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private static Logger logger = Logger.getLogger("avantiaLogger");
	
	/**
	 * Instancia de la Clase {@link BdEjecucion} que maneja los tipos de
	 * transacciones, que podemos realizar contra la base de datos.
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private BdEjecucion ejecucion = new BdEjecucion();
	
	/**
	 * Lista de objetos por numeros para procesar
	 * */
	private List<DepuracionPorNumero> paraProcesarData;
	
	/**
	 * Seccion de lectura de los numeros de telefono
	 * @author Edwin Mejia - Avantia Consultores
	 * @throws Exception 
	 * 
	 * */
	public HashMap<String, List<LogDepuracion>> procesarDepuracion()
	{				
		// iniciamos el listado de respuestas 
		setRespuestas(new ArrayList<LogDepuracion>());
		
		// si el listado de objetos a procesar esta vacio solo retornamos vacio
		if(getParaProcesarData().isEmpty())
			return new HashMap<String, List<LogDepuracion>>();
		
		//iniciamos el tiempo para verificar el procesamiento del pool
		long init = System.currentTimeMillis();
		
		//contador para conocer las ejecuciones realizadas
		int contadorPorSegundo = 0;
		
		//pool de threads para ejecutar seccionado los hilos
		ExecutorService executor = Executors.newFixedThreadPool(getParaProcesarData().size());
		
		HashMap<String, List<LogDepuracion>> respuestaOut = new HashMap<String, List<LogDepuracion>>();
		try 
		{	
			//ejecutamos lo hilos por numero
			for (DepuracionPorNumero porNumero : getParaProcesarData()) 
			{
				List<LogDepuracion> data = new ArrayList<LogDepuracion>();
				contadorPorSegundo++;
				Future<List<LogDepuracion>> future = executor.submit(porNumero);
				data = future.get();
				getRespuestas().addAll(data);

				respuestaOut.put(porNumero.getAgregador().getNombre_agregador(), data);
				
				//cada 10 segundos verificamos cuantas peticiones se han procesado
				if(((System.currentTimeMillis() - init)/1000)>10){
					logger.info("Se han procesado hasta el momento " + contadorPorSegundo + " de " + getParaProcesarData().size());
					init = System.currentTimeMillis();
				}
			}
			executor.shutdown();
		} 
		catch (Exception e) 
		{
			// Tuvimos un error al estar esperando las respuestas obtenidas desde el hilo por numero
			guardarRespuesta(ESTADO_ERROR, "Se tuvo una excepcion en el momento de estar esperando las respuestas por numero");
			executor.shutdown();
		}
		
		try 
		{
			//retornamos las respuestas
			return respuestaOut;
		} 
		catch (Exception e) 
		{
			logger.error("No se pudo poner el resultado en el hashmap", e);
			return new HashMap<String, List<LogDepuracion>>();
		}
	}
	
	/**
	 * Metodo que servira para unificar el guardado de respuesta
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param metodo
	 * @param respuesta
	 * @param estado
	 * @return {@link Void}
	 * */
	private void guardarRespuesta(String estado, String descripcion)
	{
		LogDepuracion objGuardar = new LogDepuracion();
		objGuardar.setNumero("00000");
		objGuardar.setEstadoTransaccion(estado);
		objGuardar.setFechaTransaccion(new Date());
		objGuardar.setIdMetodo(4);
		objGuardar.setRespuestaFK(null);
		objGuardar.setEnvio("");
		objGuardar.setRespuesta("");
		objGuardar.setTipoTransaccion((getParaProcesarData().size()>0?getParaProcesarData().get(0).getTipoDepuracion():"generic error"));
		objGuardar.setUsuarioSistema((getParaProcesarData().size()>0?getParaProcesarData().get(0).getUsuarioSistema():getEjecucion().usuarioMaestro()));
		objGuardar.setDescripcionEstado(descripcion);
		
		getEjecucion().createData(objGuardar);
		
		getRespuestas().add(objGuardar);
	}
	
	/**
	 * @return the respuestas
	 */
	private List<LogDepuracion> getRespuestas() 
	{
		return respuestas;
	}

	/**
	 * @param respuestas the respuestas to set
	 */
	private void setRespuestas(List<LogDepuracion> respuestas) 
	{
		this.respuestas = respuestas;
	}
	
	/**
	 * @return the ejecucion
	 */
	private BdEjecucion getEjecucion() 
	{
		return ejecucion;
	}

	/**
	 * @return the paraProcesarData
	 */
	public List<DepuracionPorNumero> getParaProcesarData() 
	{
		return paraProcesarData;
	}

	/**
	 * @param paraProcesarData the paraProcesarData to set
	 */
	public void setParaProcesarData(List<DepuracionPorNumero> paraProcesarData) 
	{
		this.paraProcesarData = paraProcesarData;
	}
}