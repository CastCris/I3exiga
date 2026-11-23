package br.edu.ifsp.i3exiga.web.view;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/** {@code <v:if test="${...}">}: renderiza o corpo apenas se a condição for verdadeira. */
public class IfTag extends SimpleTagSupport {

    private boolean test;

    public void setTest(boolean test) {
        this.test = test;
    }

    @Override
    public void doTag() throws JspException, IOException {
        if (test) {
            getJspBody().invoke(null);
        }
    }
}
