package bitlap.sbt.analyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author 梦境迷离
 * @version 1.0, 2023/8/3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependencyGraphEdge {
    Integer _gvid;
    Integer tail;
    Integer head;
    
    String label;
    public DependencyGraphEdge() {
    }


    public Integer get_gvid() {
        return _gvid;
    }

    public void set_gvid(Integer _gvid) {
        this._gvid = _gvid;
    }

    public Integer getTail() {
        return tail;
    }

    public void setTail(Integer tail) {
        this.tail = tail;
    }

    public Integer getHead() {
        return head;
    }

    public void setHead(Integer head) {
        this.head = head;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
