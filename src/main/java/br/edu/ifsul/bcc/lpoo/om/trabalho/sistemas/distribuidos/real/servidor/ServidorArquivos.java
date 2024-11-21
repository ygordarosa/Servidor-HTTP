package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class ServidorArquivos {

    int porta;
    ServerSocket servidorSocket;
    private Map<String, Arquivos> arquivos = new HashMap<>();
    private Map<String, String> chavesDeSessao = new HashMap<>();  // Armazena as chaves de sessão
    private Estado estado;

    public ServidorArquivos(int porta) {
        this.porta = porta;
    }

    public void criaServerSocket() throws IOException {
        servidorSocket = new ServerSocket(porta);
    }

    private String gerarChaveDeSessao() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    private boolean estaAutenticado(String chave) {
        return chavesDeSessao.containsValue(chave);
    }

    public Socket esperaConexao() throws IOException {
        System.out.println("Esperando conexão...");
        return servidorSocket.accept();
    }

    public void trataProtocolo(Socket socket) throws IOException {
        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true)) {

            Estado estado = Estado.CONECTADO;

            // Conteúdo de exemplo
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
                    System.out.println(arquivo.getTipo());
                    saida.println("HTTP/1.1 200 OK");
                   
                    saida.println("Content-Type: "+ arquivo.getTipo() +"; charset=UTF-8");
                    if(arquivo.getTipo().equals("image/png")){
                        System.out.println("imagem");
                        byte[] imageBytes = Base64.getDecoder().decode(arquivo.getConteudo());
                        saida.println("Content-Length: " + imageBytes.length);
                        saida.println("Connection: close");
                        saida.println();
                        socket.getOutputStream().write(imageBytes);
                    }
                    else{
                        saida.println("Content-Length: " + resposta.length());
                        saida.println("Connection: close");
                        saida.println();
                        saida.println(resposta); 
                    }
                    
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
            else if (mensagem.startsWith("POST /login")) {
                // Autenticação de login
                String linha;
                int contentLength = 0;
                while (!(linha = entrada.readLine()).isEmpty()) {
                    if (linha.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(linha.split(" ")[1].trim());
                    }
                }

                char[] corpo = new char[contentLength];
                entrada.read(corpo, 0, contentLength);
                String conteudoCorpo = new String(corpo);

                JSONObject json = new JSONObject(conteudoCorpo);
                String username = json.optString("username");
                String password = json.optString("password");

                if ("admin".equals(username) && "senha123".equals(password)) {
                    String chaveSessao = gerarChaveDeSessao();
                    chavesDeSessao.put(username, chaveSessao);

                    saida.println("HTTP/1.1 200 OK");
                    saida.println("Content-Type: application/json");
                    saida.println();
                    saida.println("{\"chave\": \"" + chaveSessao + "\"}");
                    estado = Estado.AUTENTICADO;
                } else {
                    saida.println("HTTP/1.1 403 Forbidden");
                    saida.println("Content-Type: text/plain; charset=UTF-8");
                    saida.println("Content-Length: 13");
                    saida.println();
                    saida.println("Acesso negado");
                }
            } else if (mensagem.startsWith("POST /arquivos")) {
                // Verificação de autenticação para requisições POST
                String linha;
                int contentLength = 0;
                String chaveSessao = null;

                while (!(linha = entrada.readLine()).isEmpty()) {
                    if (linha.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(linha.split(" ")[1].trim());
                    } else if (linha.startsWith("Authorization:")) {
                        chaveSessao = linha.split(" ")[1].trim();
                    }
                }

                if (chaveSessao == null || !estaAutenticado(chaveSessao)) {
                    saida.println("HTTP/1.1 403 Forbidden");
                    saida.println("Content-Type: text/plain; charset=UTF-8");
                    saida.println("Content-Length: 13");
                    saida.println();
                    saida.println("Acesso negado");
                    return;
                }

                if (contentLength <= 0) {
                    saida.println("HTTP/1.1 411 Length Required");
                    saida.println("Content-Type: text/plain; charset=UTF-8");
                    saida.println("Content-Length: 24");
                    saida.println();
                    saida.println("Content-Length ausente");
                    return;
                }

                char[] corpo = new char[contentLength];
                entrada.read(corpo, 0, contentLength);
                String conteudoCorpo = new String(corpo);

                JSONObject json = new JSONObject(conteudoCorpo);
                String name = json.optString("name");
                String content = json.optString("content");
                String type = json.optString("type");

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
