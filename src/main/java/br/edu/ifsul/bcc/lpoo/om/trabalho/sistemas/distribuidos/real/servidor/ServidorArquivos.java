package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;


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
                // Extrai o valor do parâmetro `caminho`
                if (caminhoRecurso.contains("?caminho=")) {
                    caminhoRecurso = caminhoRecurso.split("caminho=")[1];
                }
            
            System.out.println("Caminho do recurso: " + caminhoRecurso);
                Arquivos arquivo = arquivos.get(caminhoRecurso);
                String resposta;
                if (arquivo != null) {
                    resposta = arquivo.getConteudo();
                    saida.println("HTTP/1.1 200 OK");
                    saida.println("Content-Type: "+ arquivo.getTipo() +"; charset=UTF-8");
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
            else if (mensagem.startsWith("POST")) {
            String linha;
            int contentLength = 0;
            while (!(linha = entrada.readLine()).isEmpty()) {
                if (linha.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(linha.split(" ")[1]);
                }
            }

            char[] corpo = new char[contentLength];
            entrada.read(corpo, 0, contentLength);
            String conteudoCorpo = new String(corpo);

            try {
                JSONObject json = new JSONObject(conteudoCorpo);
                String name = json.optString("name");
                String content = json.optString("content");
                String type = json.optString("type");
                
                System.out.println(name + "\n" + content + "\n" + type);

                if (!name.isEmpty() && !content.isEmpty() && !type.isEmpty()) {
                    if (arquivos.containsKey(name)) {
                        String resposta = "Arquivo já existe";
                        saida.println("HTTP/1.1 409 Conflict");
                        saida.println("Content-Type: text/plain; charset=UTF-8");
                        saida.println("Content-Length: " + resposta.length());
                        saida.println();
                        saida.println(resposta);
                    } else {
                        arquivos.put(name, new Arquivos(name, content, type));
                        String resposta = "Arquivo criado com sucesso";
                        saida.println("HTTP/1.1 201 Created");
                        saida.println("Content-Type: " + type + "; charset=UTF-8");
                        saida.println("Content-Length: " + resposta.length());
                        saida.println();
                        saida.println(resposta);
                    }
                } else {
                    saida.println("HTTP/1.1 400 Bad Request");
                    saida.println("Content-Type: text/plain; charset=UTF-8");
                    saida.println("Connection: close");
                    saida.println("Content-Length: 20");
                    saida.println();
                    saida.println("Dados do POST incompletos");
                }
            } catch (Exception e) {
                saida.println("HTTP/1.1 400 Bad Request");
                saida.println("Content-Type: text/plain; charset=UTF-8");
                saida.println("Connection: close");
                saida.println("Content-Length: 18");
                saida.println();
                saida.println("Erro ao processar JSON");
            }
            
                
        }
        System.out.println(arquivos);
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
