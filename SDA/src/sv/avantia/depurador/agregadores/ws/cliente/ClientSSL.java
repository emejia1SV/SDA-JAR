package sv.avantia.depurador.agregadores.ws.cliente;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sv.avantia.depurador.agregadores.entidades.Metodos;
import sv.avantia.depurador.agregadores.hilo.Consultar;
import sv.avantia.depurador.agregadores.jdbc.BdEjecucion;
import sv.avantia.depurador.agregadores.utileria.ErroresSDA;

import com.cladonia.xml.webservice.soap.SOAPClient;

public class ClientSSL extends Consultar {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) 
	{
		String xml = "<soapenv:Envelope xmlns:loc=\"http://www.csapi.org/schema/parlayx/blackgray/v1_0/local\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"><wsse:Username>PA00000737</wsse:Username><wsse:Password Type=\"...#PasswordDigest\">vC5O8w5U2maOn+AQtNgP//O+Svc=</wsse:Password><wsse:Nonce>5faba4d8-adcd-4e44-817d-f691724591d8</wsse:Nonce><wsse:Created>2014-11-28T19:54:10Z</wsse:Created></wsse:UsernameToken></wsse:Security><tns:RequestSOAPHeader xmlns:tns=\"http://www.huawei.com.cn/schema/common/v2_1\"><tns:AppId>35000001000001</tns:AppId><tns:TransId>2014011716010012345</tns:TransId><tns:OA>50433126502</tns:OA><tns:FA>50433126502</tns:FA></tns:RequestSOAPHeader></soapenv:Header><soapenv:Body><loc:deleteGrayList><loc:version>1.0</loc:version><loc:grayList><grayee><msisdn>50433126502</msisdn></grayee></loc:grayList></loc:deleteGrayList></soapenv:Body></soapenv:Envelope>";
		
		try {
			List<Metodos>listado = (List<Metodos>)(List<?>) new BdEjecucion().listData("FROM SDA_METODOS WHERE ID = 221 ");
			if(!listado.isEmpty()){
				ClientSSL xyz = new ClientSSL();
				xyz.talk("https://hub.americamovil.com/sag/services/blackgrayService", xml, listado.get(0));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Metodo que nos carga un certificado digital
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link Void}
	 * */
	static public void doTrustToCertificates() throws Exception {
		// Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		TrustManager[] trustAllCerts = new TrustManager[] { 
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}
		
					public void checkServerTrusted(X509Certificate[] certs,
							String authType) throws CertificateException {
						return;
					}
		
					public void checkClientTrusted(X509Certificate[] certs,
							String authType) throws CertificateException {
						return;
					}
				} 
		};

		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String urlHostName, SSLSession session) {
				if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
					System.out.println("Warning: URL host '" + urlHostName	+ "' is different to SSLSession host '"	+ session.getPeerHost() + "'.");
				}
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}
	

	/**
	 * Metodo que realiza el envio del archivo request y espera el archivo
	 * response para el metodo web
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param urlval
	 *            - {@link String}
	 * @param inputMessage
	 *            - {@link String}
	 * @return {@link Void}
	 * */
	public  void talk(String urlEndpoint, String inputMessage, Metodos metodo) {
		try {

			// Trust to certificates
			doTrustToCertificates();
			
			Document doc = invoke(metodo, 120000);//toDocument2(rp);
			
			// View input
			System.out.println("-------");
			System.out.println("Soap response:");
			System.out.println(getStringFromDocument(doc));
			
			lecturaCompleta(doc, "ns1:resultCode");
		} 
		catch (Exception e) 
		{
			System.out.println(e.getMessage());
		}
	}
	

	
	private static void lecturaCompleta(Document doc, String nodeNameToReader) {
		doc.getDocumentElement().normalize();
		if (doc.getDocumentElement().hasChildNodes()) {
			NodeList nodeList = doc.getDocumentElement().getChildNodes();
			readerList(nodeList, nodeNameToReader);
		}
	}

	private static void readerList(NodeList nodeList, String nodeNameToReader) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if(node.getNodeName().equals(nodeNameToReader))
					System.out.println(node.getTextContent());//esto debere guardar
				
				if (node.hasChildNodes())
					readerList(node.getChildNodes(), nodeNameToReader);
			}
		}
	}

	
	
	private Document response = null;
	private SOAPClient client = null;
	private URL url = null;
	public  Document invoke(Metodos metodo, long timeOutMillisecond)
	{
		Document docRequest = getdocumentFromString(metodo.getInputMessageText());;

		if(docRequest==null)
			return xmlErrorSDA(ErroresSDA.ERROR_PASANDO_DE_CADENA_TEXTO_A_DOCUMENT);
		
		// create the saaj based soap client
		try {
			client = new SOAPClient(docRequest);
		/*} catch (SOAPException e) {
			logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT.getDescripcion() + " " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_SOAP_CLIENT);*/
		} catch (com.cladonia.xml.webservice.soap.SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// set the SOAPAction
		client.setSOAPAction(metodo.getSoapActionURI());

		// get the url
		try {
			url = new URL(metodo.getEndPoint());
		} catch (MalformedURLException e) {
			logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO.getDescripcion() + " " + metodo.getEndPoint() + " - " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CREAR_ENDPOINT_CON_EL_INSUMO_OBTENIDO);
		}
		
		try {
			response = client.send(url);
		} catch (com.cladonia.xml.webservice.soap.SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
		
		
		
        /*Thread taskInvoke;
		
		Runnable run = new Runnable() {
			public void run() {
				try 
				{
		        	// Tratar respuesta del servidor
					response = client.send(url);
			    } 
				catch (Exception e) 
				{
					logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD.getDescripcion() + " " + e.getMessage(),e);
			        response = xmlErrorSDA(ErroresSDA.ERROR_AL_INVOCAR_EL_METODO_SIN_SEGURIDAD);
				}
			}

		};

		taskInvoke = new Thread(run, "ServicioWeb"+metodo.getMetodo());
		taskInvoke.start();

 	try{
        	long endTimeOut = System.currentTimeMillis() + timeOutMillisecond;
        	logger.debug("tiempo parametrizado" + timeOutMillisecond);
        	while (true) 
			{
				if (response != null) 
					break;

				if (System.currentTimeMillis() > endTimeOut) 
				{
					logger.error(getAgregador().getNombre_agregador() + " SE GENERO TIMEOUT EXCEPCION INVOCAR EL METODO SIN SEGURIDAD");
					if (response == null)
						response = xmlErrorSDA(ErroresSDA.ERROR_TIMEUP_EXCEPTION);
					taskInvoke.stop();
					break;
				}
			}
			if (taskInvoke.isAlive()) 
			{
				taskInvoke.stop();
			}
			return response;
		} catch (Exception e) {
			logger.error(getAgregador().getNombre_agregador() + " " + ErroresSDA.ERROR_AL_CONSULTAR_TIMEUP_EN_EL_METODO_SIN_SEGURIDAD.getDescripcion() + " " + e.getMessage(), e);
			return xmlErrorSDA(ErroresSDA.ERROR_AL_CONSULTAR_TIMEUP_EN_EL_METODO_SIN_SEGURIDAD);
		}	*/		
		
	}
}