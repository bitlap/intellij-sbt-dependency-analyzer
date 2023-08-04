package bitlap.sbt.analyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author 梦境迷离
 * @version 1.0, 2023/8/3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependencyGraphObject {
    Integer _gvid;
    String name;

    public DependencyGraphObject() {
    }

    public Integer get_gvid() {
        return _gvid;
    }

    public void set_gvid(Integer _gvid) {
        this._gvid = _gvid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
