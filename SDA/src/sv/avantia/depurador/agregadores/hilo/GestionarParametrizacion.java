package sv.avantia.depurador.agregadores.hilo;

import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import sv.avantia.depurador.agregadores.entidades.Agregadores;
import sv.avantia.depurador.agregadores.entidades.LogDepuracion;
import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.entidades.Pais;
import sv.avantia.depurador.agregadores.entidades.ParametrosSistema;
import sv.avantia.depurador.agregadores.entidades.UsuarioSistema;
import sv.avantia.depurador.agregadores.jdbc.BdEjecucion;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;
import sv.avantia.depurador.agregadores.utileria.Log4jInit;

public class GestionarParametrizacion {

	/**
	 * Iniciar la configuracion para los apender del LOG4J
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	static {
		Log4jInit.init();
	}

	/**
	 * Obtener el appender para la impresi�n en un archivo de LOG
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private static Logger logger = Logger.getLogger("avantiaLogger");
	
	/**
	 * Instancia de las operaciones con la base de datos.
	 * 
	 * */
	private BdEjecucion ejecucion = null;

	/**
	 * Instancia de un {@link HashMap} para mantener en memoria los parametros
	 * con los que me serviran de insumo para llenar los parametros requeridos
	 * por los agregadores
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private HashMap<String, String> parametrosData = null;
	
	private List<String> numerosErroneos = new ArrayList<String>();
	
	/**
	 * Bandera que nos servira para conocer las veces en que se ha recibido
	 * respuestas desde la depuracion por numero a traves de hilo de ejecucion
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private int contadorRespuestasObtendas = 0;
	
	private ConsultaAgregadorPorHilo agregadorPorHilo;
	
	/**
	 * Metodo que inicializara todo el flujo del JAR ejecutable
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param args
	 */
	public String depuracionBajaMasiva(UsuarioSistema usuario, List<String> moviles, String tipoDepuracion, boolean obtenerRespuesta) {
		List<String> numerosPorPais = new ArrayList<String>();
		List<ConsultaAgregadorPorHilo> hilosParaEjecutar = new ArrayList<ConsultaAgregadorPorHilo>();
		
		try 
		{			
			//iniciar la instancia a las operaciones a la base de datos
			setEjecucion(new BdEjecucion());
			
			//llenar los parametros iniciales
			llenarParametros();
			
			logger.info("Obtener Parametrizaci�n");
			// consultar la parametrizaci�n
			for (Pais pais : obtenerParmetrizacion()) 
			{
				//validamos que el estado de pais a verificar este activo
				if(pais.getEstado().intValue()==1)
				{
					//verificamos la cantidad de moviles recibidos para la depuraci�n
					if (moviles.size() > 0) 
					{
						// iniciamos la nueva lista de numero por pais
						numerosPorPais = new ArrayList<String>();
						
						//recorremos los numeros obtenidos para su clasificacion por pais
						for (String string : moviles) 
						{
							if(string.length()<=8){
								//debo colocar una respuesta de error por numero no valido siempre y cuando el numero sea diferente
								if (!numerosErroneos.contains(string)) {
									numerosErroneos.add(string);
								}								
							}else{
								// para reconocer el pais lo hacemos a trav�s de su codigo de pais 
								if(string.startsWith(pais.getCodigo()))
									numerosPorPais.add(string);
							}
						}
						
						//si no hay numeros en el pais recorrido no se debe enviar ningun hilo
						if (numerosPorPais.size() > 0)
						{
							//recorremos cada aregador para levantar un hilo por agregador por pais
							for (Agregadores agregador : pais.getAgregadores()) 
							{
								//verificamos el estado del agregador que este activo para ser tomado en cuenta en la depuraci�n
								if(agregador.getEstado().intValue()==1)
								{
									//verificammos que por lo menos un agregador este parametrizado con metodos
									if(!agregador.getMetodos().isEmpty())
									{	
										logger.info(agregador.getId());
										logger.info(agregador.getNombre_agregador());
										
										// abrir un hilo pr cada agregador parametrizados
										ConsultaAgregadorPorHilo hilo = new ConsultaAgregadorPorHilo();
										hilo.setMoviles(numerosPorPais);
										hilo.setAgregador(agregador);
										hilo.setTipoDepuracion(tipoDepuracion);
										hilo.setUsuarioSistema((usuario==null?getEjecucion().usuarioMaestro():usuario));
										hilo.setParametrosData(getParametrosData());
										
										hilosParaEjecutar.add(hilo);
									}
								}
							}
						}
					}
				}				
			}	

			for (ConsultaAgregadorPorHilo consultaAgregadorPorHilo : hilosParaEjecutar)  
			{	
	        	//future = executor.submit(porNumero);
	        	Thread taskInvoke;
				Runnable run = new Runnable() 
				{
					public void run() 
					{
						try 
						{
							ExecutorService executor = Executors.newSingleThreadExecutor();
							Future<HashMap<String, List<LogDepuracion>>> future = executor.submit(agregadorPorHilo);
							guardarRespuestaEnContenedor(future.get());
							contadorRespuestasObtendas++;
					        executor.shutdown();
						} 
						catch (Exception ex) 
						{
							logger.error("Error al obtener el listado de respuestas", ex);
						}
					}
				};
				
				agregadorPorHilo = consultaAgregadorPorHilo;
				taskInvoke = new Thread(run);
				taskInvoke.start();
				Thread.sleep(100);
			}
			
	        
			//nos quedamos esperando todas las respuestas
			while(true)
			{
				Thread.sleep(500);//para no ejecutar tantas veces la misma preguntadera
				//hasta que las tengamos todas las respuestas dejamos de esperar
				if(contadorRespuestasObtendas >= hilosParaEjecutar.size())
					break;
			}
	        
			//generamos la respuesta como la tengamos
			return generar((usuario==null?getEjecucion().usuarioMaestro():usuario), tipoDepuracion);
		} 
		catch (Exception e) 
		{
			logger.error("Error en el sistema de depuracion masiva automatico ", e);
			e.printStackTrace();
			return xmlError(ErroresSDA.ERROR_GENERICO);
		}
		finally
		{
			moviles = null;
			setEjecucion(null);
		}
	}	
	
	private void guardarRespuestaEnContenedor(HashMap<String, List<LogDepuracion>> respuestasObtenidas)
	{
		if(respuestasObtenidas.size()>0)
			getData().putAll(respuestasObtenidas);	
	}
	
	private HashMap<String, List<LogDepuracion>> data = new HashMap<String, List<LogDepuracion>>();
	
	private HashMap<String, List<LogDepuracion>> getData() 
	{
		return this.data;
	}
	
	private String generar(UsuarioSistema usuario, String tipoDepuracion) throws ParserConfigurationException, TransformerException 
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		
		// Create from whole cloth
		Element root = (Element) document.createElement("respuesta");
		document.appendChild(root);
		
		Iterator<Entry<String, List<LogDepuracion>>> it = getData().entrySet().iterator();
		while(it.hasNext())
		{
			Entry<String, List<LogDepuracion>> entry = it.next();
			String agregadorNombre = entry.getKey().replaceAll(" ", "_").replaceAll("\\(", "").replaceAll("\\)", "");
			Element agregador = (Element) document.createElement(agregadorNombre);
			Element depuracion = (Element) document.createElement("Depuracion");
			
			root.appendChild(agregador);
			agregador.appendChild(depuracion);
			
			for (LogDepuracion depuracionX : entry.getValue()) 
			{
				Element metodo = (Element) document.createElement(depuracionX.getRespuestaFK().getMetodo().getMetodo().intValue()==1?"listaNegra":depuracionX.getRespuestaFK().getMetodo().getMetodo().intValue()==2?"consulta":depuracionX.getRespuestaFK().getMetodo().getMetodo().intValue()==3?"baja":"Default");
				Element numero = (Element) document.createElement("numero");
				Element codigoError = (Element) document.createElement("estado");
				Element descripcionEstado = (Element) document.createElement("descripcionEstado");
				
				depuracion.appendChild(metodo);
				
				metodo.appendChild(numero);
				metodo.appendChild(codigoError);
				metodo.appendChild(descripcionEstado);
				
				numero.appendChild(document.createTextNode(depuracionX.getNumero()));
				codigoError.appendChild(document.createTextNode(depuracionX.getEstadoTransaccion()));
				descripcionEstado.appendChild(document.createTextNode(depuracionX.getDescripcionEstado()));				
			}
		}
		
		for (String numeroErroneo : numerosErroneos) {
			//agregare las respuesta de error genericas obtenidas en la obtencion de parametrizacion
			String estado = "Error";
			String descripcion = "Numero invalido revisar su longitud y verificar tenga codigo de pais anexado";
			String agregadorNombre = "Error_Parametrizacion";
			Element agregador = (Element) document.createElement(agregadorNombre);
			Element depuracion = (Element) document.createElement("Depuracion");
			
			root.appendChild(agregador);
			agregador.appendChild(depuracion);
			
			Element metodo = (Element) document.createElement("Default");
			Element numero = (Element) document.createElement("numero");
			Element codigoError = (Element) document.createElement("estado");
			Element descripcionEstado = (Element) document.createElement("descripcionEstado");
			
			depuracion.appendChild(metodo);
			
			metodo.appendChild(numero);
			metodo.appendChild(codigoError);
			metodo.appendChild(descripcionEstado);
			
			numero.appendChild(document.createTextNode(numeroErroneo));
			codigoError.appendChild(document.createTextNode(estado));
			descripcionEstado.appendChild(document.createTextNode(descripcion));	
			
			guardarRespuesta(usuario, tipoDepuracion, estado, descripcion, numeroErroneo);
		}
		
		document.getDocumentElement().normalize();		
		return xmlOut(document);		
	}
	
	/**
	 * Metodo Para darle Salida al archivo document recibido como parametro
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param document
	 * @return void
	 * @throws javax.xml.transform.TransformerException
	 * */
	private String xmlOut( org.w3c.dom.Node document)  
			throws javax.xml.transform.TransformerException 
	{
		// usamos una fabrica de transformacion para la salida del document
		javax.xml.transform.TransformerFactory tFactory = javax.xml.transform.TransformerFactory.newInstance();
		javax.xml.transform.Transformer transformer;
		try 
		{
			transformer = tFactory.newTransformer();
		} 
		catch (javax.xml.transform.TransformerConfigurationException e1) 
		{
			throw new javax.xml.transform.TransformerConfigurationException("error en la fabrica de transformaci�n");
		}
		
		StringWriter writer = new StringWriter();
		
		try 
		{
			// cargamos nuestro insumo para la transformacion
			javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(document);
			//transformacion
			transformer.transform(source, new StreamResult(writer));
		} 
		catch (javax.xml.transform.TransformerException e) 
		{
			throw new javax.xml.transform.TransformerException("error en la transformaci�n");
		}
		
		logger.debug("Respuesta enviada en xml");
		logger.debug(writer.getBuffer().toString());
		
		return writer.getBuffer().toString();
	}
	
	public String xmlError(ErroresSDA error)
	{
		return "<respuesta><errorSDA><codigoError>"+error.getCodigo()+"</codigoError><descripcionEstado>"+error.getDescripcion()+"</descripcionEstado></errorSDA></respuesta>";
	}
	
	/**
	 * Obtener insumo de parametrizaci�n para consultar a los agregadores
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link List} paises con sus dependencias en la base de datos
	 * @throws Exception
	 *             podria generarse una exepcion en el momento de ejecutar la
	 *             consulta a la base de datos
	 * */
	@SuppressWarnings("unchecked")
	private List<Pais> obtenerParmetrizacion() throws Exception 
	{
		return (List<Pais>)(List<?>) getEjecucion().listData("FROM SDA_PAISES WHERE STATUS = 1");
	}

	/**
	 * getter
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return the ejecucion
	 */
	private BdEjecucion getEjecucion() 
	{
		return ejecucion;
	}

	/**
	 * setter
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param ejecucion
	 *            the ejecucion to set
	 * @return {@link Void}
	 */
	private void setEjecucion(BdEjecucion ejecucion) 
	{
		this.ejecucion = ejecucion;
	}

	
	/**
	 * Llena los parametros que serviran de insumo para la invocacion de los
	 * metodos web y estos se mantendran en memoria para agregar mas parametros
	 * mas adelante, en este caso se llenan primeramente con los de la tabla de
	 * la base de datos SDA_PARAMETROS_SISTEMA, luego se ejan unos explicitos
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param movil {@link String}
	 * @param metodo {@link Metodos}
	 * @return {@link Void}
	 * */
	@SuppressWarnings("unchecked")
	private void llenarParametros() throws NoSuchAlgorithmException 
	{	
		//iniciamos el listado de parametros con los que se trataran los mensajes de envio para los servicios web
		setParametrosData(new HashMap<String, String>());
		
		//colocamos el numero en una lista en memoria para que este listo por cualquier exepciones
		//getParametrosData().put("movil", movil);
		
		//se consultan los parametros del sistema de la base de datos 
		//porque de ahi podremos tener una inyeccion automatica de parametros a los mensajes de envio
		List<ParametrosSistema> parametrosSistemas = (List<ParametrosSistema>) (List<?>)getEjecucion().listData("FROM SDA_PARAMETROS_SISTEMA");
		for (ParametrosSistema parametrosSistema : parametrosSistemas) 
		{
			// se colocan los parametros del sistema a la data que nos servira de insumo para los parametros web
			getParametrosData().put(parametrosSistema.getDato(), parametrosSistema.getValor());
		}
		getParametrosData().put("date", new Date().toString());
		getParametrosData().put("fecha", new Date().toString());
		getParametrosData().put("dateSMT", fechaFormated());
		getParametrosData().put("nonce", java.util.UUID.randomUUID().toString());
		
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
	private void guardarRespuesta(UsuarioSistema usuario, String tipoDepuracion, String estado, String descripcion, String numero){
		LogDepuracion objGuardar = new LogDepuracion();
		objGuardar.setNumero(numero);
		objGuardar.setEstadoTransaccion(estado);
		objGuardar.setFechaTransaccion(new Date());
		objGuardar.setIdMetodo(4);
		objGuardar.setRespuestaFK(null);
		objGuardar.setEnvio("Revisar el logSDA para verificar el texto exacto de insumo");
		objGuardar.setRespuesta("");
		objGuardar.setTipoTransaccion(tipoDepuracion);
		objGuardar.setUsuarioSistema(usuario);
		objGuardar.setDescripcionEstado(descripcion);
		
		getEjecucion().createData(objGuardar);
		
		//getRespuestas().add(objGuardar);
	}
	
	/**
	 * Metodo que se encarga de formatear la fecha asi como fue solictado por el SMT
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link String}
	 * */
	private String fechaFormated(){
    	SimpleDateFormat dateT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    	return dateT.format(Calendar.getInstance().getTime());
    }
	
	/**
	 * @return the parametrosData
	 */
	private HashMap<String, String> getParametrosData() {
		return parametrosData;
	}

	/**
	 * @param parametrosData the parametrosData to set
	 */
	private void setParametrosData(HashMap<String, String> parametrosData) {
		this.parametrosData = parametrosData;
	}
}
