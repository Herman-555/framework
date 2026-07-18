package mg.itu.framework.mapping;

public class UrlMethod {
    private String url;
    private String method;

    public UrlMethod(String url, String method) {
        this.url = url;
        this.method = method != null ? method.toUpperCase() : null;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UrlMethod)) return false;
        UrlMethod urlMethod = (UrlMethod) o;
        return url != null ? url.equals(urlMethod.url) : urlMethod.url == null &&
               method != null ? method.equals(urlMethod.method) : urlMethod.method == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }
}
