package br.edu.ifsp.i3exiga.service;

/** O usuário não tem permissão para a operação (HTTP 403). */
public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccessDeniedException() {
        super();
    }

    public AccessDeniedException(String message) {
        super(message);
    }
}
