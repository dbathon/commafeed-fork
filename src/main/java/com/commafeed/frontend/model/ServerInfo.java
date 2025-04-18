package com.commafeed.frontend.model;

import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.ApiClass;
import java.io.Serializable;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("serial")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@ApiClass("Server infos")
public class ServerInfo implements Serializable {

  private String announcement;
  private String version;
  private Map<String, String> supportedLanguages = Maps.newHashMap();

  public String getAnnouncement() {
    return announcement;
  }

  public void setAnnouncement(String announcement) {
    this.announcement = announcement;
  }

  public Map<String, String> getSupportedLanguages() {
    return supportedLanguages;
  }

  public void setSupportedLanguages(Map<String, String> supportedLanguages) {
    this.supportedLanguages = supportedLanguages;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

}
