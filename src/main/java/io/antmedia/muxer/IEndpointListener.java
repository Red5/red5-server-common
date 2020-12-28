package io.antmedia.muxer;

public interface IEndpointListener {
    public void setEndpointStatus(String url, String status);
    public String getEndpointStatus(String url);
}
