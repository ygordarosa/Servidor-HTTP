/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 *
 * @author Caroline
 */
public class Arquivos {
    private String nome;
    private String conteudo;
    private String tipo;
    
    public Arquivos(String nome, String conteudo, String tipo) {
        this.nome = nome;
        this.conteudo = conteudo;
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public String getConteudo() {
        return conteudo;
    }
    
    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

}
