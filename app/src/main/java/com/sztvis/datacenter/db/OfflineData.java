package com.sztvis.datacenter.db;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

@Entity
public class OfflineData {
    @Id(autoincrement = true)
    private Long id;
    private String url;
    private String data;
    private static final long serialVersionUID = 1L;
  
    @Generated(hash = 2100497485)
    public OfflineData() {
    }
    @Generated(hash = 841005779)
    public OfflineData(Long id, String url, String data) {
        this.id = id;
        this.url = url;
        this.data = data;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
