package com.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends Thread {

    private static final String[][] TIPI = {
            { "txt", "text/plain; charset=utf-8" },
            { "md", "text/markdown; charset=utf-8" },
            { "csv", "text/csv; charset=utf-8" },
    };

    private final Socket client;

    public ClientHandler(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try (Socket c = client) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(c.getOutputStream(), StandardCharsets.UTF_8), true);

            String richiesta = in.readLine();
            if (richiesta == null) {
                return;
            }

            String pulita = richiesta.trim();
            if (pulita.isEmpty()) {
                out.println("ERR SYNTAX");
                out.println();
                return;
            }

            String[] parti = pulita.split("\\s+");
            if (!"LEGGI".equals(parti[0])) {
                out.println("ERR UNKNOWNCMD");
                out.println();
                return;
            }

            boolean nomeAmmesso = parti.length == 2
                    && !parti[1].isEmpty()
                    && !parti[1].contains(".")
                    && !parti[1].contains("/")
                    && !parti[1].contains("\\");
            if (!nomeAmmesso) {
                out.println("ERR SYNTAX");
                out.println();
                return;
            }

            File file = null;
            String tipoMime = null;
            for (int i = 0; i < TIPI.length; i++) {
                String estensione = TIPI[i][0];
                File candidato = new File("documenti", parti[1] + "." + estensione);
                if (candidato.isFile()) {
                    file = candidato;
                    tipoMime = TIPI[i][1];
                    break;
                }
            }
            if (file == null) {
                out.println("ERR NOTFOUND");
                out.println();
                return;
            }

            boolean statoInviato = false;
            try (BufferedReader lettore = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                out.println("OK FILE");
                out.println("Content-Type: " + tipoMime);
                out.println();
                statoInviato = true;

                String riga;
                while ((riga = lettore.readLine()) != null) {
                    out.println(riga);
                }
            } catch (IOException e) {
                if (!statoInviato) {
                    out.println("ERR READ");
                    out.println();
                } else {
                    System.err.println("Errore di lettura: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
