package framework.type;

public class Fichier {
    private String name;
    private String extension;
    private byte[] binary;

    public Fichier() {}

    public Fichier(String name, String extension, byte[] binary) {
        this.name = name;
        this.extension = extension;
        this.binary = binary;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public byte[] getBinary() {
        return binary;
    }

    public void setBinary(byte[] binary) {
        this.binary = binary;
    }
}