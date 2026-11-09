package com.limelight.artemis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Every toast goes through ArtemisToast, so it's in Orbix (the system draws plain text toasts in its own font). */
public class ArtemisToastTest {
    private static final Pattern PLAIN_TOAST = Pattern.compile("(?<![\\w.])Toast\\.makeText\\(");

    @Test
    public void noToastSkipsArtemisToast() throws IOException {
        Path src = Paths.get("src/main/java");
        assertTrue("run from the app module: " + src.toAbsolutePath(), Files.isDirectory(src));
        List<String> plain = new ArrayList<>();
        try (Stream<Path> files = Files.walk(src)) {
            for (Path p : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (p.getFileName().toString().equals("ArtemisToast.java")) {
                    continue;
                }
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (PLAIN_TOAST.matcher(lines.get(i)).find()) {
                        plain.add(src.relativize(p) + ":" + (i + 1));
                    }
                }
            }
        }
        assertEquals("plain Toast.makeText (use ArtemisToast.makeText)", new ArrayList<String>(), plain);
    }
}
