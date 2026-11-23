package br.edu.ifsp.i3exiga.service;

/** Erro de validação cuja mensagem é exibida ao usuário. */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException() {
        super();
    }

    public ValidationException(String message) {
        super(message);
    }
}
