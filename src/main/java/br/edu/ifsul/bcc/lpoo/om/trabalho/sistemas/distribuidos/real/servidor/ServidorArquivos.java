package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServidorArquivos {

    int porta;
    ServerSocket servidorSocket;
    private Map<String, Arquivos> arquivos = new HashMap<>();
    private Estado estado;

    public ServidorArquivos(int porta) {
        this.porta = porta;
    }

    public void criaServerSocket() throws IOException {
        servidorSocket = new ServerSocket(porta);
    }

    public Socket esperaConexao() throws IOException {
        System.out.println("Esperando conexão...");
        return servidorSocket.accept();
    }

    public void trataProtocolo(Socket socket) throws IOException {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true)) {

            Estado estado = Estado.CONECTADO;
            
            String indexText = "<!DOCTYPE html>\n" +
                                "<html lang=\"en\">\n" +
                                "<head>\n" +
                                "  <meta charset=\"UTF-8\">\n" +
                                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                                "  <title>Document</title>\n" +
                                "</head>\n" +
                                "<body>\n" +
                                "  <p>bem vindo</p>\n" +
                                "</body>\n" +
                                "</html>";
            String indexName = "index.html";
            String indexType = "text/html";
            arquivos.put(indexName, new Arquivos(indexName, indexText, indexType));

            String mensagem = entrada.readLine();
            if (mensagem == null) {
                estado = Estado.FINALIZADO;
                return;
            }
            
            if (mensagem.startsWith("GET")) {
                String[] parts = mensagem.split(" ");
                String caminhoRecurso = parts[1].substring(1); // Remove o primeiro "/"
                Arquivos arquivo = arquivos.get(caminhoRecurso);

                String resposta;
                if (arquivo != null) {
                    resposta = arquivo.getConteudo();
                    saida.println("HTTP/1.1 200 OK");
                    saida.println("Content-Type: text/html; charset=UTF-8");
                    saida.println("Content-Length: " + resposta.length());
                    saida.println("Connection: close");
                    saida.println();
                    saida.println(resposta);
                } else {
                    resposta = "Arquivo não encontrado";
                    saida.println("HTTP/1.1 404 Not Found");
                    saida.println("Content-Type: text/plain; charset=UTF-8");
                    saida.println("Content-Length: " + resposta.length());
                    saida.println("Connection: close");
                    saida.println();
                    saida.println(resposta);
                }
            }
            if (mensagem.startsWith("POST")){
                

            }
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            ServidorArquivos server = new ServidorArquivos(3000);
            server.criaServerSocket();
            while (true) {
                Socket socket = server.esperaConexao();
                server.trataProtocolo(socket);
            }
        } catch (IOException e) {
            System.out.println("Erro ao processar a conexão do cliente: " + e.getMessage());
        }
    }
}
