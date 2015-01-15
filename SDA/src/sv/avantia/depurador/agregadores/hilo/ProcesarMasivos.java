package sv.avantia.depurador.agregadores.hilo;

import java.util.ArrayList;
import java.util.List;

import sv.avantia.depurador.agregadores.entidades.Clientes_Tel;

public class ProcesarMasivos implements Runnable {

	private List<String> telefonos = new ArrayList<String>();
	private List<Clientes_Tel> clientesTelsUpdate = new ArrayList<Clientes_Tel>();

	@Override
	public void run() {
		try {
			// varios numeros al mismo tiempo
			// gestionar la logica de la depuracion
			GestionarParametrizacion gestion = new GestionarParametrizacion();
			gestion.setClientesTelsUpdate(getClientesTelsUpdate());
			gestion.depuracionBajaMasiva(null, getTelefonos(), "MASIVA", false);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * @return the telefonos
	 */
	private List<String> getTelefonos() {
		return telefonos;
	}

	/**
	 * @param telefonos
	 *            the telefonos to set
	 */
	public void setTelefonos(List<String> telefonos) {
		this.telefonos = telefonos;
	}

	/**
	 * @return the clientesTelsUpdate
	 */
	private List<Clientes_Tel> getClientesTelsUpdate() {
		return clientesTelsUpdate;
	}

	/**
	 * @param clientesTelsUpdate
	 *            the clientesTelsUpdate to set
	 */
	public void setClientesTelsUpdate(List<Clientes_Tel> clientesTelsUpdate) {
		this.clientesTelsUpdate = clientesTelsUpdate;
	}

}
