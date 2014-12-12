package sv.avantia.depurador.agregadores.servicio.web;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import sv.avantia.depurador.agregadores.entidades.LogDepuracion;
import sv.avantia.depurador.agregadores.entidades.UsuarioSistema;
import sv.avantia.depurador.agregadores.hilo.GestionarParametrizacion;
import sv.avantia.depurador.agregadores.jdbc.BdEjecucion;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

/**
 * Clase que realizara el procesar el XML de insumo con numeros moviles y
 * gestionar la depuracion masiva
 * 
 * @author Edwin Mejia - Avantia Consultores
 * @version 1.0
 * */
public class EjecucionServicioWeb {

	private List<String> listaMoviles = new ArrayList<String>();
	private GestionarParametrizacion gestion = new GestionarParametrizacion();
	private String respuesta;
	
	/**
	 * Instancia de las operaciones con la base de datos.
	 * 
	 * */
	private BdEjecucion ejecucion = new BdEjecucion();
	
	/**
	 * Obtener el appender para la impresión en un archivo de LOG
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private static Logger logger = Logger.getLogger("avantiaLogger");

	/**
	 * Metodo que realizara la gestion de la depuracion apuntando al codigo que
	 * ya se tiene dentro del SDA
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param moviles
	 *            {@link String} en formato XML
	 * @return {@link String} en formato XML
	 * */
	public String respuestaBajaMasivas(String moviles) {
		String tipoDepuracion = "Depuracion Servicio Web";
		try 
		{
			leerInsumos(moviles, tipoDepuracion);
		} catch (Exception e) {
			if (getRespuesta() == null)
				setRespuesta(getGestion().xmlError(ErroresSDA.ERROR_GENERICO));
			return getRespuesta();
		}
		
		try {
			if (getRespuesta() != null){
				logger.debug("Respuesta enviada en xml");
				logger.debug(getRespuesta());
				return getRespuesta();
			}
			
			return getGestion().depuracionBajaMasiva(null, getListaMoviles(),	tipoDepuracion, true);
		} finally {
			setGestion(null);
		}
	}

	/**
	 * Metodo que realizara el ingreso de numeros a la lista negra con codigo
	 * que ya se tiene dentro del SDA
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param moviles
	 *            {@link String} en formato XML
	 * @return {@link String} en formato XML
	 * */
	public String respuestaIngresoListaNegra(String moviles) {
		String tipoDepuracion = "Alta Servicio Web";
		try 
		{
			leerInsumos(moviles, tipoDepuracion);
		} catch (Exception e) {
			if (getRespuesta() == null)
				setRespuesta(getGestion().xmlError(ErroresSDA.ERROR_GENERICO));
			return getRespuesta();
		}
		
		try {
			if (getRespuesta() != null){
				logger.debug("Respuesta enviada en xml");
				logger.debug(getRespuesta());
				return getRespuesta();
			}
			
			// el nombre 'Alta Servicio Web' de sirve de bandera para realizar el alta
			return getGestion().depuracionBajaMasiva(null, getListaMoviles(),	tipoDepuracion, true);
		} finally {
			setGestion(null);
		}
	}

	/**
	 * Metodo que leera el archivo XML donde vienen los numeros moviles a
	 * depurar
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param insumoXML
	 *            {@link String} en formato XML
	 * @return {@link List} de numeros mviles
	 * */
	private void leerInsumos(String insumoXML, String tipoDepuracion) {

		//impresion de insumo recibido
		logger.info("insumo recibido: ");
		logger.info(insumoXML);
		
		if(insumoXML.length()>=11){
			logger.error(ErroresSDA.ERROR_EN_LA_LECTURA_DE_INSUMOS_VERIFICAR_ESTRUCTURA_SOLICTADA_PARA_CONSUMO_SERVICIO_WEB.getDescripcion());
			
			guardarRespuesta(getEjecucion().usuarioMaestro(), tipoDepuracion, "Error", ErroresSDA.ERROR_EN_LA_LECTURA_DE_INSUMOS_VERIFICAR_ESTRUCTURA_SOLICTADA_PARA_CONSUMO_SERVICIO_WEB.getDescripcion(), "sin procesar");
			
			// si no esta nula es porque si se obtuvo antes
			if (getRespuesta() == null)
				setRespuesta(getGestion().xmlError(ErroresSDA.ERROR_EN_LA_LECTURA_DE_INSUMOS_VERIFICAR_ESTRUCTURA_SOLICTADA_PARA_CONSUMO_SERVICIO_WEB));
			return;
		}
		
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = builderFactory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(
					new ByteArrayInputStream(insumoXML.getBytes("utf-8"))));
			doc.getDocumentElement().normalize();
			if (doc.getDocumentElement().hasChildNodes()) {
				NodeList nodeList = doc.getDocumentElement().getChildNodes();
				setListaMoviles(new ArrayList<String>());
				lecturaListaTelefonos(nodeList, "movil");
			}
		} catch (Exception e) {
			logger.error(ErroresSDA.ERROR_EN_LA_LECTURA_DE_INSUMOS_VERIFICAR_ESTRUCTURA_SOLICTADA_PARA_CONSUMO_SERVICIO_WEB.getDescripcion(), e);
			
			// si no esta nula es porque si se obtuvo antes
			if (getRespuesta() == null)
				setRespuesta(getGestion().xmlError(ErroresSDA.ERROR_EN_LA_LECTURA_DE_INSUMOS_VERIFICAR_ESTRUCTURA_SOLICTADA_PARA_CONSUMO_SERVICIO_WEB));
		}
	}

	/**
	 * Metodo recursivo, para la lectura de nodos del Soap Response que se ha
	 * recibido de la consulta de los {@link Agregadores} a su vez este metodo
	 * se encarga de guardar en la base de datos as {@link Respuesta} obtenidas
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param nodeList
	 *            {@link NodeList}
	 * @param nodeNameToReader
	 *            {@link String} nombre del nodo que andamos buscando dentro del
	 *            listado
	 * @return {@link Void}
	 * @throws Exception
	 * */
	private void lecturaListaTelefonos(NodeList nodeList,
			String nodeNameToReader) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (node.getNodeName().equalsIgnoreCase(nodeNameToReader)) {
					if (node.getFirstChild() != null
							|| node.getFirstChild().getNodeValue() != null) {
						listaMoviles.add(node.getTextContent());
					}
				}
				// recursive
				if (node.hasChildNodes())
					lecturaListaTelefonos(node.getChildNodes(),
							nodeNameToReader);
			}
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
	 * @return the listaMoviles
	 */
	private List<String> getListaMoviles() {
		return listaMoviles;
	}

	/**
	 * @param listaMoviles
	 *            the listaMoviles to set
	 */
	private void setListaMoviles(List<String> listaMoviles) {
		this.listaMoviles = listaMoviles;
	}

	/**
	 * @return the gestion
	 */
	private GestionarParametrizacion getGestion() {
		return gestion;
	}

	/**
	 * @param gestion
	 *            the gestion to set
	 */
	private void setGestion(GestionarParametrizacion gestion) {
		this.gestion = gestion;
	}

	/**
	 * @return the respuesta
	 */
	private String getRespuesta() {
		return respuesta;
	}

	/**
	 * @param respuesta
	 *            the respuesta to set
	 */
	private void setRespuesta(String respuesta) {
		this.respuesta = respuesta;
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
}
