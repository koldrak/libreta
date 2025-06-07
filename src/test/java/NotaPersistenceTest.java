import org.junit.Test;
import org.junit.Assert;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class NotaPersistenceTest {
    @Test
    public void testGuardarYCargarNotasConArchivoTemporal() throws Exception {
        Path tempDir = Files.createTempDirectory("notas_test");
        String originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        try {
            ArrayList<Nota> notas = new ArrayList<>();
            notas.add(new Nota("temp", "contenido"));
            Nota.guardarNotas(notas);

            ArrayList<Nota> cargadas = Nota.cargarNotas();
            Assert.assertEquals(1, cargadas.size());
            Assert.assertEquals("temp", cargadas.get(0).titulo);
            Assert.assertEquals("contenido", cargadas.get(0).contenidoHTML);
        } finally {
            System.setProperty("user.dir", originalUserDir);
        }
    }
}
