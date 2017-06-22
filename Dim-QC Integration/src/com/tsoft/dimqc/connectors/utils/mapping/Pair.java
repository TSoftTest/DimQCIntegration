package ar.com.tssa.serena.connectors.utils.mapping;

public class Pair<qCAttribute, dimAttribute> {
    private qCAttribute attributeQC = null;
    private dimAttribute attributeDim = null;

    public Pair(qCAttribute attributeQC, dimAttribute attributeDim) {
        this.attributeQC = attributeQC;
        this.attributeDim = attributeDim;
    }

    public qCAttribute getQCAttribute() {
        return attributeQC;
    }

    public void setQCAttribute(qCAttribute attributeQC) {
        this.attributeQC = attributeQC;
    }

    public dimAttribute getDimAttribute() {
        return attributeDim;
    }

    public void setDimAttribute(dimAttribute attributeDim) {
        this.attributeDim = attributeDim;
    }

}