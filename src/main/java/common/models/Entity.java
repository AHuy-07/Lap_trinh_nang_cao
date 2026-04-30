package common.models;

abstract class Entity {
    private String id;
    private String name;

    public Entity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Begin: Setter/getter
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
    // End: Setter/Getter
}
