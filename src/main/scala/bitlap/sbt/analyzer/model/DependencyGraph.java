package bitlap.sbt.analyzer.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author 梦境迷离
 * @version 1.0, 2023/8/3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DependencyGraph {
    String name;
    Boolean directed;
    Boolean strict;
    List<DependencyGraphObject> objects;
    List<DependencyGraphEdge> edges;


    public DependencyGraph() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDirected() {
        return directed;
    }

    public void setDirected(Boolean directed) {
        this.directed = directed;
    }

    public Boolean getStrict() {
        return strict;
    }

    public void setStrict(Boolean strict) {
        this.strict = strict;
    }

    public List<DependencyGraphObject> getObjects() {
        return objects;
    }

    public void setObjects(List<DependencyGraphObject> objects) {
        this.objects = objects;
    }

    public List<DependencyGraphEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<DependencyGraphEdge> edges) {
        this.edges = edges;
    }
}
