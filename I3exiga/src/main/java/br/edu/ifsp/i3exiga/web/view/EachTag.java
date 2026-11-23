package br.edu.ifsp.i3exiga.web.view;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Arrays;

/** {@code <v:each items="${lista}" var="item">}: repete o corpo para cada elemento da coleção. */
public class EachTag extends SimpleTagSupport {

    private Iterable<?> items;
    private String var;

    /** Aceita coleções e arrays (por exemplo, {@code Language.values()}). */
    public void setItems(Object items) {
        if (items instanceof Iterable<?> iterable) {
            this.items = iterable;
        } else if (items instanceof Object[] array) {
            this.items = Arrays.asList(array);
        } else {
            this.items = null;
        }
    }

    public void setVar(String var) {
        this.var = var;
    }

    @Override
    public void doTag() throws JspException, IOException {
        if (items == null) {
            return;
        }
        for (Object item : items) {
            getJspContext().setAttribute(var, item);
            getJspBody().invoke(null);
        }
        getJspContext().removeAttribute(var);
    }
}
