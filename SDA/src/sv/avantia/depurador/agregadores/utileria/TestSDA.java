package sv.avantia.depurador.agregadores.utileria;

import java.util.ArrayList;
import java.util.List;

import sv.avantia.depurador.agregadores.hilo.GestionarParametrizacion;

public class TestSDA {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long init =System.currentTimeMillis();
		try {
			List<String> moviles = new ArrayList<String>();
			//moviles.add("50257128343");
			moviles.add("50433126502");
			//moviles.add("50257128545");
			//moviles.add("50257128747");
			//moviles.add("50257128848");
			//moviles.add("50257129050");
			
			GestionarParametrizacion gestion = new GestionarParametrizacion();
			System.out.println(gestion.depuracionBajaMasiva(null, moviles,"Servicio Web", true));
		} finally {
			System.out.println("tiempo definitivo" + (System.currentTimeMillis() - init));
		}
		
	}
}
