package br.edu.ifsp.i3exiga.model;

import java.io.Serializable;

/** Caso de teste de um problema: entrada fornecida e saída esperada. */
public class TestCase implements Serializable {

    private static final long serialVersionUID = 1L;

    private String input;
    private String expectedOutput;

    public TestCase() {
    }

    public TestCase(String input, String expectedOutput) {
        this.input = input;
        this.expectedOutput = expectedOutput;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }
}
