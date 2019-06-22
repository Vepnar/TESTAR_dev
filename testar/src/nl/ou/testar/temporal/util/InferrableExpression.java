package nl.ou.testar.temporal.util;

public  enum InferrableExpression {
    value_eq_("number"),
    value_lt_("number"),
    textmatch_("text"),
    heigth_lt_("shape"),
    width_lt_("shape"),
    textlength_eq_("text"),
    textlength_lt_("text");


    public final String typ;

    private InferrableExpression(String typ) {
        this.typ=typ;
    }
};

