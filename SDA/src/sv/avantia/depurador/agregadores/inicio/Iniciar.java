package sv.avantia.depurador.agregadores.inicio;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import sv.avantia.depurador.agregadores.entidades.Clientes_Tel;
import sv.avantia.depurador.agregadores.hilo.ProcesarMasivos;
import sv.avantia.depurador.agregadores.jdbc.BdEjecucion;
import sv.avantia.depurador.agregadores.utileria.Log4jInit;

public class Iniciar {

	/**
	 * Iniciar la configuracion para los apender del LOG4J
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	static {
		Log4jInit.init();
	}

	/**
	 * Obtener el appender para la impresión en un archivo de LOG
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * */
	private static Logger logger = Logger.getLogger("avantiaLogger");
	
	/**
	 * Instancia de las operaciones con la base de datos.
	 * 
	 * */
	private static BdEjecucion ejecucion = null;

	/**
	 * Metodo que inicializara todo el flujo del JAR ejecutable
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @param args
	 */
	public static void main(String[] args) 
	{
		long init 								= System.currentTimeMillis(); 	//medir el tiempo
		List<String> telefonos 					= new ArrayList<String>();		//lista de string de telefonos por que es el standard
		List<Clientes_Tel> clientes_Tels 		= new ArrayList<Clientes_Tel>();	//es la lista completa obtenida como insumo
		List<Clientes_Tel> clientesTelsUpdate 	= new ArrayList<Clientes_Tel>();	//lista de telefonos que se van a actualizar - deberia ser un mismo numero el que viaja en esta lista
		
		try 
		{
			logger.info("Iniciando la depuración Masiva...");
			
			//iniciar la instancia a las operaciones a la base de datos
			setEjecucion(new BdEjecucion());
			
			//obtener los numeros de insumo
			clientes_Tels = obtenerInsumo();
			
			int intervalos = 10;
			int cantidadparcial = 10;
			int inicial = 0;
			List<ProcesarMasivos> numerosRunable = new ArrayList<ProcesarMasivos>();
			
			while(true)
			{
				telefonos = new ArrayList<String>();
				clientesTelsUpdate = new ArrayList<Clientes_Tel>();
				
				if(clientes_Tels.size()>=cantidadparcial)
				{
					//procesar por bloques pequeños de numeros
					for (int i = inicial; i < cantidadparcial; i++) 
					{
						telefonos.add(clientes_Tels.get(i).getNumero());
						clientesTelsUpdate.add(clientes_Tels.get(i));
					}
					cantidadparcial = cantidadparcial + intervalos;
					inicial = inicial + intervalos;
					
					//gestionar la logica de la depuracion
					ProcesarMasivos runClass = new ProcesarMasivos();
					runClass.setClientesTelsUpdate(clientesTelsUpdate);
					runClass.setTelefonos(telefonos);
					numerosRunable.add(runClass);
					
				}
				else
				{
					//procesar por bloques pequeños de numeros
					for (int i = inicial; i < clientes_Tels.size(); i++) 
					{
						telefonos.add(clientes_Tels.get(i).getNumero());
						clientesTelsUpdate.add(clientes_Tels.get(i));
					}
					
					//gestionar la logica de la depuracion
					ProcesarMasivos runClass = new ProcesarMasivos();
					runClass.setClientesTelsUpdate(clientesTelsUpdate);
					runClass.setTelefonos(telefonos);
					numerosRunable.add(runClass);
					break;
				}
			}
			
			ExecutorService executor = Executors.newFixedThreadPool(10);
			for (ProcesarMasivos procesarMasivos : numerosRunable) {
				executor.execute(procesarMasivos);
			}
			executor.shutdown();
			System.out.println("Finished all threads");
		} 
		catch (Exception e) 
		{
			logger.error("Error en el sistema de depuracion masiva automatico ", e);
		}
		finally
		{
			setEjecucion(null);
			telefonos = null;
			clientes_Tels = null;
			logger.info("finalizo la depuración Masiva de los numeros en " + ((System.currentTimeMillis() - init)/1000)  + "Segundos");
		}
	}
	
	/**
	 * Obtener insumo de parametrización para consultar a los agregadores
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return {@link List} paises con sus dependencias en la base de datos
	 * @throws Exception
	 *             podria generarse una exepcion en el momento de ejecutar la
	 *             consulta a la base de datos
	 * */
	@SuppressWarnings("unchecked")
	private static List<Clientes_Tel> obtenerInsumo() throws Exception 
	{
		return (List<Clientes_Tel>)(List<?>) getEjecucion().listData("FROM CLIENTE_TEL WHERE ESTADO = 0");
	}
	
	/**
	 * getter
	 * 
	 * @author Edwin Mejia - Avantia Consultores
	 * @return the ejecucion
	 */
	private static BdEjecucion getEjecucion() {
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
	private static void setEjecucion(BdEjecucion ejecucion) {
		Iniciar.ejecucion = ejecucion;
	}
}